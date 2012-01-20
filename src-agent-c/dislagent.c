#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdarg.h>
#include <sys/time.h>

#include <stddef.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <sys/types.h>
#include <netdb.h>
#include <netinet/tcp.h>

#include <unistd.h>

#include <sys/ipc.h>
#include <sys/shm.h>
#include <sys/stat.h>

#include <jvmti.h>
#include <jni.h>

#include "dislagent.h"

static int total = 0;
static long contention = 0;

static int globalKey = 8000; // init value 
static int SHM_SIZE_TIMES = 5;


static int SOCKET_IPC = 0;

static jvmtiEnv *jvmti = NULL;

static unsigned char MAGIC_JBORAT[4] = {0xDA,0x6D,0xA6,0x00};

// port and name of the instrumentation server
static int port_number;
static char* host_name;

static int DEBUG = 0;
static int STATS = 0;
static int LATENCY = 0;
static int VERBOSE = 0;

typedef struct {
    unsigned char* instrumented_class;
    jint instrumented_class_size;
} instrumented_data;

typedef struct  {
    //   unsigned char magic[4]; // 0XDA6DA600
    unsigned char namesize[4]; // int to hold class name size
    unsigned char datalen[4]; // int to hold the size of the class file
} jborat_header;

typedef struct {
    /* JVMTI Environment */
    jvmtiEnv      *jvmti;
    jboolean       vm_is_started;
    /* Data access Lock */
    jrawMonitorID  lock;
} GlobalAgentData;


// Info about classloaders
struct jborat_classloader {
    int counter;
    char * classLoaderName;
    jobject CLWeakRef; // weak ref to the classLoader
    struct jborat_classloader * next;
};

// linked list to hold socket file descriptor
// access must be protected by monitor
struct sock_node {
    int sockfd;
    int available;
    int counter;
  long latency;
   jlong start_nanos;
  jlong end_nanos;
    struct sock_node *next;
};

typedef struct jborat_classloader jbcl;
typedef struct sock_node jbsock;



// the first element on the list of socket connections
static jbcl *classloader_list = NULL;

// the first element on the list of socket connections
static jbsock *sock_list = NULL;

static GlobalAgentData *gdata;


int createConnection() {
    struct sockaddr_in sin;
    struct sockaddr_in pin;
    struct hostent *hp;
    int newsockfd = 0;

    hp = gethostbyname(host_name);
    memset(&pin, 0, sizeof(pin));
    pin.sin_family = AF_INET;
    pin.sin_addr.s_addr = ((struct in_addr *)(hp->h_addr))->s_addr;
    pin.sin_port = htons(port_number);

    /* grab an Internet domain socket */
    if ((newsockfd = socket(/*AF_INET*/PF_INET, SOCK_STREAM,/* 0*/ IPPROTO_TCP)) == -1) {
        perror("socket");
        exit(1);
    }

    /* connect to PORT on HOST */
    if (connect(newsockfd,(struct sockaddr *)  &pin, sizeof(pin)) == -1) {
        perror("connect");
        exit(1);
    }

    int flag = 1;
    if (setsockopt(newsockfd, IPPROTO_TCP, TCP_NODELAY, (char *)&flag, sizeof(int)) <0) {
        perror("nagle");
        exit(10);
    }
    return newsockfd;
}

/* Every JVMTI interface returns an error code, which should be checked
 *   to avoid any cascading errors down the line.
 *   The interface GetErrorName() returns the actual enumeration constant
 *   name, making the error messages much easier to understand.
 */
static void
check_jvmti_error(jvmtiEnv *jvmti, jvmtiError errnum, const char *str)
{
    if ( errnum != JVMTI_ERROR_NONE ) {
        char       *errnum_str;

        errnum_str = NULL;
        (void)(*jvmti)->GetErrorName(jvmti, errnum, &errnum_str);

        printf("ERROR: JVMTI: %d(%s): %s\n", errnum, (errnum_str==NULL?"Unknown":errnum_str), (str==NULL?"":str));
    }
}

/* Enter a critical section by doing a JVMTI Raw Monitor Enter */
static void
enter_critical_section(jvmtiEnv *jvmti)
{
    jvmtiError error;

    error = (*jvmti)->RawMonitorEnter(jvmti, gdata->lock);
    check_jvmti_error(jvmti, error, "Cannot enter with raw monitor");
}

/* Exit a critical section by doing a JVMTI Raw Monitor Exit */
static void
exit_critical_section(jvmtiEnv *jvmti)
{
    jvmtiError error;

    error = (*jvmti)->RawMonitorExit(jvmti, gdata->lock);
    check_jvmti_error(jvmti, error, "Cannot exit with raw monitor");
}

/* for getting options (arguments) for the agent */
char* get_token(char *str, char *seps, char *buf, int max)
{
    int len;
    buf[0] = 0;
    if (str == NULL || str[0] == 0)
        return NULL;
    str += strspn(str, seps);
    if (str[0] == 0)
        return NULL;
    len = (int) strcspn(str, seps);
    if (len >= max) {
        printf("problem reading token for options!!");
        exit(-1);
    }
    strncpy(buf, str, len);
    buf[len] = 0;
    return str + len;
}



static void
parse_agent_options(char *options)
{
#define MAX_TOKEN_LENGTH        32
    char  token[MAX_TOKEN_LENGTH];
    char *next;

    /* Defaults */

    /* Parse options and set flags in gdata */
    if ( options==NULL ) {
        return;
    }

    /* Get the first token from the options string. */

    next = get_token(options, ",", token, (int)sizeof(token));
    if(next ==NULL){
        printf("---ERROR:Specify the port and the host!---\n");
        exit(1);
    }

    // convert string port to int
    port_number=atoi(token);

    next = get_token(next, ",", token, (int)sizeof(token));
    if(next ==NULL){
        printf("---ERROR:Specify the port and the host!---\n");
        exit(1);
    }

    host_name=(char*)malloc(MAX_TOKEN_LENGTH);

    strcpy(host_name,token);
    if(VERBOSE) {
      printf("Will connect to server::%s on port %d\n",host_name, port_number);
    }
    next = get_token(next, ",", token, (int)sizeof(token));

    char *ipc_type;
    if(next == NULL) {
      if(VERBOSE) {
	printf("using shared memory..IPC\n");
      }

    }else{
      ipc_type = (char*)malloc(MAX_TOKEN_LENGTH);
      strcpy(ipc_type,token);
      if(strcmp(ipc_type, "ipc.socket") == 0) {
	if(VERBOSE) {
	  printf("using socket IPC...\n");
	}
	SOCKET_IPC = 1;
      }else{
	if(strcmp(ipc_type, "ipc.shm") == 0) {
	  printf("using shared memory IPC...\n");
	  ipc_type = (char*)malloc(MAX_TOKEN_LENGTH);
	  strcpy(ipc_type,token);
	}else{
	  printf(" Unknown IPC, supported 'ipc.socket' or 'ipc.shm' (default) \n");
	  exit(-1);
	}
      }
    }

}

/* SEND FUNCTION */
int sendData(int sockfd, char *datatosend, int tosend, char *descr, char *detail) {
    int sent = 0;
    int res = 0;
    

    while(sent != tosend) {

        res = send(sockfd,datatosend + sent , (tosend - sent) ,0);
        if(res == -1) {
            perror("Send error");
            printf("ERROR IN TRANSMISSION (send) !!! %s  %s \n", descr, detail);
            return -1;
        }else {
            if(res == 0) {
                printf("WARNING : send returned 0  (send) %s %s \n", descr, detail);
            }
            sent += res;

            if(DEBUG) {
                printf("**** SENDING  MORE %d  FROM %d \n " , sent, tosend);
            }
        }
    }
    if(DEBUG){
        printf("Total sent %d : expected  %d\n", sent, tosend);
    }


    return 0;
}

/* RECEIVE FUNCTION */ 
int rcvData(int sockfd, char *datatorcv, int toreceive, char *descr, char *detail) {
    int res_count = 0;
    int res = 0;
    while(res_count != toreceive) {

        res = recv(sockfd, datatorcv + res_count, (toreceive - res_count) ,0);

        if(res == -1) {

            printf("values: total %d missing  %d done %d\n", toreceive, (toreceive - res_count), res_count);
            perror("Receive error");
            printf("ERROR IN TRANSMISSION!!! recv %s %s \n", descr, detail);
            return -1;
        }else {
            if(res == 0) {
                printf("WARNING : rcv returned 0 for %s %s \n", descr, detail);
            }
            res_count += res;
            if(DEBUG)
                printf("**** RECEIVING MORE %d FROM %d \n " , res_count, toreceive);
        }
    }
    if(DEBUG)
        printf("Total received %d : expected  %d\n", res_count, toreceive);
    return 0;
}

static int getNbDigits(int n) {
  int count = 0;
  while(n > 0) { n /= 10;count++;}
  return count;
}

static int genUniqueKey() {
  int theKey;
  enter_critical_section(jvmti); {
    theKey = (globalKey++);
  }exit_critical_section(jvmti);
  return theKey;
}

static char* genKey(int theKey, int size, int times) {
  int bufsize = getNbDigits(theKey) + getNbDigits(size) +  getNbDigits(times) + 2;
  char *buffer = malloc(bufsize);
  sprintf(buffer, "%d,%d,%d", theKey,size,times);
  return buffer;
}

static instrumented_data* instrument_class_SHM(int sockfd, const char* name, const unsigned char* class_data, jint class_data_len){

    // HEADER TO SEND DATA
    jborat_header *hdr;
    hdr = (jborat_header*)malloc(sizeof(jborat_header));
    int class_name_size= strlen(name);
    memcpy(hdr->namesize,&class_name_size,sizeof(class_name_size));
    
    // the datalen 
    int key = genUniqueKey();
    char* smkeysize = genKey(key, class_data_len, SHM_SIZE_TIMES);
    int keysize_len = strlen(smkeysize);
    memcpy(hdr->datalen, &keysize_len,sizeof(keysize_len));

    int size = *((int *)hdr->namesize);

    // create a shared memory segment
    int shmid;
    char *shm;

    size_t thesize = (class_data_len * SHM_SIZE_TIMES);

    if ((shmid = shmget(key, thesize, IPC_CREAT | 0666)) < 0) {
      perror("shmget");
      exit(1);
    }
    
    if ((shm = shmat(shmid, NULL, 0)) == (char *) -1) {
      perror("shmat");
      exit(1);
    }


    // now copy the classfile in Shared Memory

    memcpy(shm, class_data, class_data_len);
    

    if(DEBUG) {
        printf("CLASSNAME SIZE %d \n", size);
    }

    int val = *((int *)hdr->datalen);
    if(DEBUG) {
        printf("CLASSFILE SIZE %d \n", val);
    }

    if(DEBUG) {
        printf("SENDING: VAL %d  CLASS_DATA_LEN %d FOR CLASS %s\n", val, class_data_len,name);
    }


    // SEND THE THREE PARTS
    if(sendData(sockfd, (char*)hdr, (int)sizeof(jborat_header),"HEADER", (char*)name) != 0) {perror("[jboratclientagent] ERROR 1"); exit(-5);};
    if(sendData(sockfd, (char*)name, class_name_size,"CLASSNAME", (char*) name) != 0) {perror("[jboratclientagent] ERROR 2"); exit(-6);};
    //    if(sendData(sockfd, (char*)class_data, class_data_len,"DATA",(char*) name) != 0) {perror("[jboratclientagent] ERROR 3"); exit(-7);};
    if(sendData(sockfd, (char*)smkeysize, keysize_len,"DATA",(char*) name) != 0) {perror("[jboratclientagent] ERROR 3"); exit(-7);};

    
    // NOW RECEIVE DATA, FIRST HEADER
    char buffer[sizeof(jborat_header)];  // 8 bytes // 12 bytes
    if(rcvData(sockfd, buffer, (int)sizeof(jborat_header), "HEADER", (char *)name) != 0) {perror("[jboratclientagent] ERROR 4"); exit(-8);};



    jborat_header *rcvhdr;
    rcvhdr = (jborat_header *)malloc(sizeof(jborat_header));
    memcpy(rcvhdr,buffer,sizeof(jborat_header));

    if(DEBUG) {
        //    printf(" +++++ THE RCV HEX %02X\n", rcvhdr->magic[0]);
        // printf(" +++++ THE RCV HEX %02X\n", rcvhdr->magic[1]);
        // printf(" +++++ THE RCV HEX %02X\n", rcvhdr->magic[2]);
        // printf(" +++++ THE RCV HEX %02X\n", rcvhdr->magic[3]);
        printf(" +++++ THE RCV HEX %02X\n", rcvhdr->namesize[0]);
        printf(" +++++ THE RCV HEX %02X\n", rcvhdr->namesize[1]);
        printf(" +++++ THE RCV HEX %02X\n", rcvhdr->namesize[2]);
        printf(" +++++ THE RCV HEX %02X\n", rcvhdr->namesize[3]);
        printf(" +++++ THE RCV HEX %02X\n", rcvhdr->datalen[0]);
        printf(" +++++ THE RCV HEX %02X\n", rcvhdr->datalen[1]);
        printf(" +++++ THE RCV HEX %02X\n", rcvhdr->datalen[2]);
        printf(" +++++ THE RCV HEX %02X\n", rcvhdr->datalen[3]);
    }

    // GET INFO FROM THE HEADER TO READ THE REST
    int rcvnamesize = *((int *)rcvhdr->namesize);

    int rcvdatalen =  *((int *)rcvhdr->datalen);

    if(DEBUG){
        printf("BINGO ! RCV NAME SIZE = %d  DATALEN = %d\n", rcvnamesize, rcvdatalen);
    }
    // now receive the classname
    char rcvClassName[rcvnamesize+1];

    if(rcvData(sockfd, rcvClassName, rcvnamesize, "CLASSNAME", (char *)name) != 0) {perror("[jboratclientagent] ERROR 5"); exit(-9);};

    // terminate string
    rcvClassName[rcvnamesize] = '\0';


    if(DEBUG) {
        printf("RCV CLASSNAME %s\n", rcvClassName);
    }

    if(rcvdatalen == 0) {
        // AN ERROR HAPPENED IN THE SERVER
        // The "className" contains the error message

        printf("\n++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n\n");
        printf(" Error occurred in the remote instrumentation server\n");
        printf(" Reason: \n");
        printf("   %s\n\n", rcvClassName);
        printf("\n++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n\n");
        exit(-1);
    }

    char *rcvDataFile = (char *)malloc(rcvdatalen);

    // now receive the data
    if(rcvData(sockfd, rcvDataFile, rcvdatalen, "DATA", (char *)name) != 0) {perror("[jboratclientagent] ERROR 6"); exit(-10);};

    // reading the size of the instrumented data from shared memory
    char insClassSize[rcvdatalen+1];
    memcpy(insClassSize, rcvDataFile, rcvdatalen);
    insClassSize[rcvdatalen] = '\0';
    int insSize = atoi(insClassSize);
    if(DEBUG) {
      printf("  %s  [%d] [%d]\n",rcvClassName, class_data_len,  insSize);
    }
    // READ FROM SHARED MEMORY SEGMENT

    char *instrDataFile = (char *)malloc(insSize);
    memcpy(instrDataFile, shm, insSize);

    //  WE CAN "CLOSE THE SEGMENT"

    if (shmctl(shmid,IPC_RMID,NULL) == -1) {
      perror("shmid");
      exit(1);
    }

    if (shmdt(shm) == -1) {
      perror("shmdt");
      exit(1);
    }


    // BUILD THE STRUCT RETURNING instrumented classfile + len
    instrumented_data* ins=(instrumented_data*)malloc(sizeof(instrumented_data));
    
    ins->instrumented_class = (unsigned char*)instrDataFile;
    ins->instrumented_class_size=insSize;

    free(hdr);
    free(rcvhdr);

    return ins;
} 




/* instruments remotely a class, it handles the communication protocol */


static instrumented_data* instrument_class(int sockfd, const char* name, const unsigned char* class_data, jint class_data_len){

    // HEADER TO SEND DATA
    jborat_header *hdr;
    hdr = (jborat_header*)malloc(sizeof(jborat_header));
    int class_name_size= strlen(name);
    memcpy(hdr->namesize,&class_name_size,sizeof(class_name_size));

    memcpy(hdr->datalen, &class_data_len,sizeof(class_data_len));

    int size = *((int *)hdr->namesize);
    if(DEBUG) {
        printf("CLASSNAME SIZE %d \n", size);
    }

    int val = *((int *)hdr->datalen);
    if(DEBUG) {
        printf("CLASSFILE SIZE %d \n", val);
    }

    if(DEBUG) {
        printf("SENDING: VAL %d  CLASS_DATA_LEN %d FOR CLASS %s\n", val, class_data_len,name);
    }


    // SEND THE THREE PARTS
    if(sendData(sockfd, (char*)hdr, (int)sizeof(jborat_header),"HEADER", (char*)name) != 0) {perror("[jboratclientagent] ERROR 1"); exit(-5);};
    if(sendData(sockfd, (char*)name, class_name_size,"CLASSNAME", (char*) name) != 0) {perror("[jboratclientagent] ERROR 2"); exit(-6);};
    if(sendData(sockfd, (char*)class_data, class_data_len,"DATA",(char*) name) != 0) {perror("[jboratclientagent] ERROR 3"); exit(-7);};

    // NOW RECEIVE DATA, FIRST HEADER
    char buffer[sizeof(jborat_header)];  // 8 bytes // 12 bytes
    if(rcvData(sockfd, buffer, (int)sizeof(jborat_header), "HEADER", (char *)name) != 0) {perror("[jboratclientagent] ERROR 4"); exit(-8);};

    jborat_header *rcvhdr;
    rcvhdr = (jborat_header *)malloc(sizeof(jborat_header));
    memcpy(rcvhdr,buffer,sizeof(jborat_header));

    if(DEBUG) {
        //    printf(" +++++ THE RCV HEX %02X\n", rcvhdr->magic[0]);
        // printf(" +++++ THE RCV HEX %02X\n", rcvhdr->magic[1]);
        // printf(" +++++ THE RCV HEX %02X\n", rcvhdr->magic[2]);
        // printf(" +++++ THE RCV HEX %02X\n", rcvhdr->magic[3]);
        printf(" +++++ THE RCV HEX %02X\n", rcvhdr->namesize[0]);
        printf(" +++++ THE RCV HEX %02X\n", rcvhdr->namesize[1]);
        printf(" +++++ THE RCV HEX %02X\n", rcvhdr->namesize[2]);
        printf(" +++++ THE RCV HEX %02X\n", rcvhdr->namesize[3]);
        printf(" +++++ THE RCV HEX %02X\n", rcvhdr->datalen[0]);
        printf(" +++++ THE RCV HEX %02X\n", rcvhdr->datalen[1]);
        printf(" +++++ THE RCV HEX %02X\n", rcvhdr->datalen[2]);
        printf(" +++++ THE RCV HEX %02X\n", rcvhdr->datalen[3]);
    }

    // GET INFO FROM THE HEADER TO READ THE REST
    int rcvnamesize = *((int *)rcvhdr->namesize);

    int rcvdatalen =  *((int *)rcvhdr->datalen);

    if(DEBUG){
        printf("BINGO ! RCV NAME SIZE = %d  DATALEN = %d\n", rcvnamesize, rcvdatalen);
    }
    // now receive the classname
    char rcvClassName[rcvnamesize+1];

    if(rcvData(sockfd, rcvClassName, rcvnamesize, "CLASSNAME", (char *)name) != 0) {perror("[jboratclientagent] ERROR 5"); exit(-9);};

    // terminate string
    rcvClassName[rcvnamesize] = '\0';

    if(DEBUG) {
        printf("RCV CLASSNAME %s\n", rcvClassName);
    }

    if(rcvdatalen == 0) {
        // AN ERROR HAPPENED IN THE SERVER
        // The "className" contains the error message

        printf("\n++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n\n");
        printf(" Error occurred in the remote instrumentation server\n");
        printf(" Reason: \n");
        printf("   %s\n\n", rcvClassName);
        printf("\n++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n\n");
        exit(-1);
    }

    char *rcvDataFile = (char *)malloc(rcvdatalen);

    // now receive the data
    if(rcvData(sockfd, rcvDataFile, rcvdatalen, "DATA", (char *)name) != 0) {perror("[jboratclientagent] ERROR 6"); exit(-10);};

    // BUILD THE STRUCT RETURNING instrumented classfile + len
    instrumented_data* ins=(instrumented_data*)malloc(sizeof(instrumented_data));
    ins->instrumented_class=(unsigned char*)rcvDataFile;
    ins->instrumented_class_size=rcvdatalen;

    free(hdr);
    free(rcvhdr);

    return ins;
} 



// make a JNI call (reflection) to get the name of the classloader
char* getClassLoaderName(JNIEnv *jni_env, jobject loader) {
    jmethodID mid;
    char *clname = NULL;

    jclass cloader = (*jni_env)->FindClass(jni_env, "java/lang/ClassLoader");
    if(cloader != NULL) {

        mid = (*jni_env)->GetMethodID(jni_env, cloader, "toString","()Ljava/lang/String;");
        if(mid != NULL) {

            jstring cloaderName = (jstring) (*jni_env)->CallObjectMethod(jni_env, loader, mid, NULL);

            jbyte *ccloaderName = (jbyte *)(*jni_env)->GetStringUTFChars(jni_env, cloaderName, NULL);
            if(ccloaderName != NULL) {
                clname = (char *)ccloaderName;

                return clname;
            }
        }
    }

    return clname;
}

// make the socket available again
void releaseSock(jbsock *s) {
    struct timeval start, end;
    long  seconds, useconds;
    if(STATS)
        gettimeofday(&start, NULL);
    enter_critical_section(jvmti); {
        if(STATS) {
            gettimeofday(&end, NULL);
            seconds = end.tv_sec - start.tv_sec;
            useconds = end.tv_usec - start.tv_usec;
            contention += (((seconds)*100 + useconds/1000.0) + 0.5);
        }

        // MAKE IT AVAILABLE AGAIN
        s->available = 1;

    }exit_critical_section(jvmti);
}

// get an available socket, create one if no one is available
// the link list requires access with the monitor
jbsock *getSock() {

    jbsock *currSock = NULL;
    jbsock *prevSock = NULL;

    // CONTENTION
    struct timeval start, end;
    long  seconds, useconds;
    if(STATS)
        gettimeofday(&start, NULL);

    enter_critical_section(jvmti);
    {
        // CONTENTION
        if(STATS) {
            gettimeofday(&end, NULL);
            seconds = end.tv_sec - start.tv_sec;
            useconds = end.tv_usec - start.tv_usec;
            contention += (((seconds)*100 + useconds/1000.0) + 0.5);
        }

        currSock = sock_list;

        while(currSock != NULL) {
            if(currSock->available == 1) {
                currSock->counter = (currSock->counter) + 1;
                currSock->available = 0;
		currSock->start_nanos = 0;
		currSock->end_nanos = 0;
		currSock->latency = 0;
                break;
            }else{
                prevSock = currSock;
                currSock = currSock->next;
            }
        }
        if(currSock == NULL) {
            // create new Socket
            jbsock *newSock = (jbsock *)malloc(sizeof(jbsock));
            prevSock->next = newSock;
            newSock->available = 0;
            newSock->counter = 1;
            newSock->sockfd = createConnection();
            newSock->next = NULL;
            currSock = newSock;
        }
    }
    exit_critical_section(jvmti);

    return currSock;
}

// linked list with the classloader info
// stores the CL name + a weak reference to the CL
jbcl *getClassLoaderNode(JNIEnv *jni_env, jobject loader) {

    if(loader == NULL) {
        // the bootclasloader
        classloader_list->counter = (classloader_list->counter) + 1;
        return classloader_list;
    } else {
        jobject currentWeakRef =  (*jni_env)->NewWeakGlobalRef(jni_env, loader);
        jbcl *currNode = classloader_list;
        jbcl *prevNode = NULL;

        while(currNode != NULL) {
            if((*jni_env)->IsSameObject(jni_env, currentWeakRef, currNode->CLWeakRef)) {
                int cc = (currNode->counter);
                cc++;
                currNode->counter = cc;
                return currNode;
            }
            else
            {
                prevNode = currNode;
                currNode = currNode->next;
            }
        }
        // not found, create a new node
        currNode  = (jbcl *)malloc(sizeof(jbcl));
        // point the last node's next of the list to the new node
        prevNode->next = currNode;
        currNode->next = NULL;
        currNode->counter = 1;
        currNode->classLoaderName = getClassLoaderName(jni_env, loader);
        currNode->CLWeakRef = currentWeakRef;
        return currNode;
    }
}




long long getmsofday() {
  struct timeval tv;
  gettimeofday(&tv, NULL);
  return (long long)tv.tv_sec*1000 + tv.tv_usec/1000;
}


static void JNICALL jvmti_callback_class_file_load_hook( jvmtiEnv *jvmti_env, JNIEnv* jni_env, jclass class_being_redefined, jobject loader,
        const char* name, jobject protection_domain, jint class_data_len, const unsigned char* class_data,
        jint* new_class_data_len, unsigned char** new_class_data)
{
  // do nothing for the moment

        *new_class_data_len = 0;
        *new_class_data = NULL;

        const unsigned char * newClassData = NULL;

        jint newLength = 0;

        // get or create a socket
        jbsock *theSock = getSock();
        int sock = theSock->sockfd;

	jlong start_nanos = 0;
	jlong end_nanos = 0;

        // ask the server to instrument

        instrumented_data* result;

	if(SOCKET_IPC) {
	  result = instrument_class(sock, name, class_data, class_data_len);
	}else{
	  result = instrument_class_SHM(sock, name, class_data, class_data_len);
	}

        // release the socket
        releaseSock(theSock);

        newClassData = result->instrumented_class;
        newLength = result->instrumented_class_size;
	
	
        if(newLength > 0) {
            if(DEBUG)
                printf(" WILL REPLACE THE CLASS...%s new length %d\n", name, newLength);

            {
                // give to JVM the instrumented class
                if(STATS) {
                    enter_critical_section(jvmti);{
                        total++;
                    }exit_critical_section(jvmti);
                }
                unsigned char *jvmtispace;

                // let JVMTI to allocate the mem for the new class
                jvmtiError err = (*jvmti_env)->Allocate(jvmti_env, (jlong)newLength, &jvmtispace);
                check_jvmti_error(jvmti_env, err, "cannot allocate");

                (void)memcpy(jvmtispace, newClassData, newLength);

                // set the newly instrumented class + len
                *(new_class_data_len) = (jint)newLength;
                *(new_class_data) = jvmtispace;

                // free memory
                free(result->instrumented_class);
                free(result);
            }
        }
	
}
	
  


// HOOK TO INSTRUMENT ALL CLASSES BEFORE LOADING
static void JNICALL jvmti_callback_class_file_load_hook_ORIG( jvmtiEnv *jvmti_env, JNIEnv* jni_env, jclass class_being_redefined, jobject loader,
        const char* name, jobject protection_domain, jint class_data_len, const unsigned char* class_data,
        jint* new_class_data_len, unsigned char** new_class_data)
{

    if(STATS) {
        enter_critical_section(jvmti); {
            getClassLoaderNode(jni_env, loader);
        }exit_critical_section(jvmti);
    }

    //  enter_critical_section(jvmti);
    {

        /*
    if(!gdata->vmDead) {
      const char * classname;

      if(name == NULL) {
    printf("WARNING class name is NULL\n");
      }else {
    classname = strdup(name);
      }
    }
         */

        *new_class_data_len = 0;
        *new_class_data = NULL;

        const unsigned char * newClassData = NULL;

        jint newLength = 0;

        // get or create a socket
        jbsock *theSock = getSock();
        int sock = theSock->sockfd;

	jlong start_nanos = 0;
	jlong end_nanos = 0;

        // ask the server to instrument
	if(LATENCY) {
	  jvmtiError err = (*jvmti_env)->GetCurrentThreadCpuTime(jvmti_env, &start_nanos);
	  check_jvmti_error(jvmti_env, err, "unable to take time");
	  theSock->start_nanos = start_nanos;
	}
        instrumented_data* result = instrument_class(sock, name, class_data, class_data_len);
	if(LATENCY) {

	  jvmtiError err = (*jvmti_env)->GetCurrentThreadCpuTime(jvmti_env, &end_nanos);
	  check_jvmti_error(jvmti_env, err, "unable to take time");
	  theSock->end_nanos = end_nanos;

	}

        // release the socket
        releaseSock(theSock);

        newClassData = result->instrumented_class;
        newLength = result->instrumented_class_size;

        if(newLength > 0) {
            if(DEBUG)
                printf(" WILL REPLACE THE CLASS...%s new length %d\n", name, newLength);

            {
                // give to JVM the instrumented class
                if(STATS) {
                    enter_critical_section(jvmti);{
                        total++;
                    }exit_critical_section(jvmti);
                }
                unsigned char *jvmtispace;

                // let JVMTI to allocate the mem for the new class
                jvmtiError err = (*jvmti_env)->Allocate(jvmti_env, (jlong)newLength, &jvmtispace);
                check_jvmti_error(jvmti_env, err, "cannot allocate");

                (void)memcpy(jvmtispace, newClassData, newLength);

                // set the newly instrumented class + len
                *(new_class_data_len) = (jint)newLength;
                *(new_class_data) = jvmtispace;

                // free memory
                free(result->instrumented_class);
                free(result);
            }
        }
    }
    //  exit_critical_section(jvmti);

}    

static void JNICALL jvmti_callback_class_vm_start_hook(jvmtiEnv *jvmti_env, JNIEnv* jni_env)
{
    //printf("====Starting the VM %s...====\n");

}    

static void jvmti_callback_class_vm_init_hook(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread)
{
    //printf("====Initializing the VM...====\n");
}

static void JNICALL jvmti_callback_class_vm_death_hook(jvmtiEnv *jvmti_env, JNIEnv* jni_env) {
    printf("====Shutting down the VM...====\n");

    if(STATS) {
        printf(" Total classes instrumented in server = %d\n",total);
        printf(" Contention counter = %ld ms. \n", contention);

        jbcl *node = classloader_list;
        int clcount = 0;
        while(node != NULL) {
            clcount++;
            if(DEBUG)
                printf("ClassLoader %s  %d  ", node->classLoaderName, node->counter);
            if((*jni_env)->IsSameObject(jni_env, node->CLWeakRef, NULL)) {
                if(DEBUG)
                    printf(" Garbage Collected \n");
            }else{
                if(DEBUG)
                    printf("\n");
            }
            node = node->next;
        }
        printf("Number of classloaders = %d\n", clcount);
    }

    jbsock *snode = sock_list;
    
    // prepare
    jborat_header *terminate_hdr;
    terminate_hdr = (jborat_header*)malloc(sizeof(jborat_header));
    int terminate_size = 0;
    memcpy(terminate_hdr->namesize, &terminate_size, sizeof(terminate_size));
    memcpy(terminate_hdr->datalen, &terminate_size, sizeof(terminate_size));
    
    // latency
    
    long totalLatency = 0;
    while(snode != NULL) {
      if(STATS) {
	printf(" sock = %d counter = %d\n", snode->sockfd,  snode->counter);
      }
      // send shutdown message
      sendData(snode->sockfd, (char*)terminate_hdr, (int)sizeof(jborat_header), "TERMINATE", "BYE");
      //
      totalLatency += snode->latency;
      
            close(snode->sockfd);
            snode = snode->next;
    }
    
    if(LATENCY) {
      printf("TOTAL LATENCY = %ld \n", totalLatency);
    }
    
    free(terminate_hdr);
    
    // free memory
    jbsock *curr = sock_list;
    jbsock *tofree = NULL;
    while(curr!=NULL) {
        tofree = curr;
        curr = curr->next;
        free(tofree);
    }

    if(STATS) {
        jbcl *currcl = classloader_list;
        jbcl *tofreecl = NULL;
        while(currcl != NULL) {
            tofreecl = currcl;
            currcl = currcl->next;
            free(tofreecl);
        }
    }

    free(host_name);
}

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *jvm, char *options, void *reserved)
{
    jvmtiEnv* jvmti_env=NULL;
    jvmtiCapabilities cap;
    jvmtiEventCallbacks callbacks;
    jvmtiError error;


    static GlobalAgentData data;

    (void)memset((void *)&data, 0, sizeof(data));
    gdata = &data;

    // read options (port/hostname)
    parse_agent_options(options);

    if(VERBOSE) {
      printf("starting remote instrumentation agent\n");
    }
    if ((*jvm)->GetEnv(jvm,(void **) &jvmti_env, JVMTI_VERSION_1_0)!=0)
    {
        printf("Error while getting jvmti_env\n");
    }

    jvmti = jvmti_env;
    gdata->jvmti = jvmti;

    // PREFIXING

    //    const char* prefix ="ch_dag_usi_jp2_";



    // adding hooks
    memset(&cap, 0, sizeof(cap));
    cap.can_generate_all_class_hook_events  = 1;

    // timer
    cap.can_get_current_thread_cpu_time = 1;

    // PREFIX
    //    cap.can_set_native_method_prefix = 1;    

    if ((*jvmti_env)->AddCapabilities(jvmti_env, &cap) != JVMTI_ERROR_NONE) fprintf(stderr, "Cannot add capabilities.");

    (void)memset(&callbacks,0, sizeof(callbacks));

    callbacks.ClassFileLoadHook = &jvmti_callback_class_file_load_hook;
    callbacks.VMStart = &jvmti_callback_class_vm_start_hook;
    callbacks.VMInit = &jvmti_callback_class_vm_init_hook;
    callbacks.VMDeath = &jvmti_callback_class_vm_death_hook;

    (*jvmti_env)->SetEventCallbacks(jvmti_env, &callbacks, (jint)sizeof(callbacks));

    error = (*jvmti_env)->SetEventNotificationMode(jvmti_env, JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, NULL); 
    check_jvmti_error(jvmti, error, "Cannot set class load hook");

    error =  (*jvmti_env)->SetEventNotificationMode(jvmti_env,JVMTI_ENABLE,JVMTI_EVENT_VM_START, NULL);
    check_jvmti_error(jvmti, error, "Cannot create jvm start hook");

    error =  (*jvmti_env)->SetEventNotificationMode(jvmti_env,JVMTI_ENABLE,JVMTI_EVENT_VM_INIT, NULL);
    check_jvmti_error(jvmti, error, "Cannot create jvm init hook");

    error =  (*jvmti_env)->SetEventNotificationMode(jvmti_env,JVMTI_ENABLE,JVMTI_EVENT_VM_DEATH, NULL);
    check_jvmti_error(jvmti, error, "Cannot create jvm death hook");

    error = (*jvmti)->CreateRawMonitor(jvmti, "agent data", &(gdata->lock));
    check_jvmti_error(jvmti, error, "Cannot create raw monitor");

    // PREFIX
    //    error = (*jvmti)->SetNativeMethodPrefix(jvmti,prefix);
    //  printf("native prefix is set to %s\n", prefix);


    if(STATS) {
        // to gather info about classloaders
        // head of the list is for bootclassloader (loader is NULL)
        classloader_list = (jbcl *)malloc(sizeof (jbcl));

        classloader_list->classLoaderName = "BOOTCLASSLOADER";
        classloader_list->CLWeakRef=NULL;
        classloader_list->counter = 1;
        classloader_list->next = NULL;

    }

    int sockfd = createConnection();

    // socket pool  list's head node
    sock_list = (jbsock *)malloc(sizeof(jbsock));
    sock_list->sockfd = sockfd;
    sock_list->available = 1;
    sock_list->counter = 0;
    sock_list->next = NULL;

    return 0;
}

