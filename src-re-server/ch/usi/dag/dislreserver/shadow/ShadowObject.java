package ch.usi.dag.dislreserver.shadow;

import java.util.Formattable;
import java.util.Formatter;

public class ShadowObject implements Formattable {

	final private long shadowId;
	final private ShadowClass shadowClass;

	private Object shadowState;

	//

    ShadowObject (final long netReference) {
        this (
            netReference, ShadowClassTable.get (
                NetReferenceHelper.get_class_id (netReference)
            )
        );
    }


    ShadowObject (final long netReference, final ShadowClass shadowClass) {
        this.shadowId = NetReferenceHelper.get_object_id (netReference);
        this.shadowClass = shadowClass;
        this.shadowState = null;
    }

    //
    
    public long getId () {
		return shadowId;
	}

    
    public ShadowClass getShadowClass () {
    	
    	if (shadowClass != null) {
    		return shadowClass;
    	} else {
    		return ShadowClassTable.JAVA_LANG_CLASS;
    	}
	}


    public synchronized Object getState () {
        return shadowState;
    }


    public synchronized <T> T getState (final Class <T> type) {
        return type.cast (shadowState);
    }


    public synchronized void setState (final Object shadowState) {
		this.shadowState = shadowState;
	}

	public synchronized Object setStateIfAbsent(Object shadowState) {

		Object retVal = this.shadowState;

		if (retVal == null) {
			this.shadowState = shadowState;
		}

		return retVal;
	}

    //
    
    @Override
    public void formatTo (
        final Formatter formatter, 
        final int flags, final int width, final int precision
    ) {
        formatter.format ("%s@%x", (shadowClass != null) ? shadowClass.getName () : "<missing>", shadowId);
    }

}
