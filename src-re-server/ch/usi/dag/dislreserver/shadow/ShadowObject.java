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
		return shadowClass;
	}


    public Object getState () {
        return shadowState;
    }


    public <T> T getState (final Class <T> type) {
        return type.cast (shadowState);
    }


    public void setState (final Object shadowState) {
		this.shadowState = shadowState;
	}

    //
    
    @Override
    public void formatTo (
        final Formatter formatter, 
        final int flags, final int width, final int precision
    ) {
        formatter.format ("%s@%x", shadowClass.getName (), shadowId);
    }

}
