/* *************************************************************************************************
 * IBMatcherException.java
 *
 * DESCRIPTION:
 *     Android Java wrapper for IBScanMatcher library
 *     http://www.integratedbiometrics.com
 *
 * NOTES:
 *     Copyright (c) Integrated Biometrics, 2013
 *     
 * HISTORY:
 *     2013/03/08  First version.
 ************************************************************************************************ */

package com.integratedbiometrics.ibscanmatcher;

/**
 * Exception thrown when error returned by IB device.
 */
@SuppressWarnings("serial")
public class IBMatcherException extends Exception
{
    // The type of this exception.
    private final Type type;

    /**
     * Enumeration representing type of exception.
     */
    public enum Type
    {
        /**
         * Invalid parameter value
         */
        INVALID_PARAM_VALUE(-1),
        /**
         * Insufficient memory
         */
        MEM_ALLOC(-2),
        /**
         * Requested functionality isn't supported
         */
        NOT_SUPPORTED(-3),
        /**
         * File open failed (e.g. usb handle, pipe, image file ,,,)
         */
        FILE_OPEN(-4),
        /**
         * File read failed (e.g. usb handle, pipe, image file ,,,)
         */
        FILE_READ(-5),
        /**
         * Failure due to a locked resource
         */
        RESOURCE_LOCKED(-6),
        /**
         * Failure due to a missing resource (e.g. DLL file)
         */
        MISSING_RESOURCE(-7),
        /**
         * Invalid access pointer address
         */
        INVALID_ACCESS_POINTER(-8),
        /**
         * Thread creation failed
         */
        THREAD_CREATE(-9),
        /**
         * Generic command execution failed
         */
        COMMAND_FAILED(-10),
        /** 
         * File save failed (e.g. usb handle, pipe, image file ,,,)
         */
        FILE_SAVE(-11),    

        /**
         * Opening the matcher failed.
         */
        OPEN_MATCHER_FAILED(-600),
        /**
         * Closing the matcher failed.
         */
        CLOSE_MATCHER_FAILED(-601),
        /**
         * No matcher instance is open.
         */
        NO_MATCHER_INSTANCE(-602),
        /**
         * The matcher handle is invalid.
         */
		INVALID_HANDLE(-603),

        /**
         * Template extraction failed.
         */
        EXTRACTION_FAILED(-604),
        /** 
         * Enrollment failed.
         */
        ENROLLMENT_FAILED(-605),
        /** 
         * Matching failed.
         */
        MATCHING_FAILED(-606),
        /**
         * Compression failed.
         */
        COMPRESSION_FAILED(-607),
        /** 
         * Decompression failed.
         */
        DECOMPRESSION_FAILED(-608),
        /**
         * Conversion failed.
         */
        CONVERT_FAILED(-609),
        /**
         * There is no data in template.
         */
        THERE_IS_NO_DATA(-610),
        /**
         * Function is not supported.
         */
        NOT_SUPPORTED_FUNCTION(-611),
        /** 
         * Image format is not supported.
         */
        NOT_SUPPORTED_IMAGE_FORMAT(-612),
        /**
         * Device type is not supported.
         */
        NOT_SUPPORTED_DEVICE_TYPE(-613),

        /**
         * ISO file is incorrect.
         */
        INCORRECT_ISO_FILE(-700);

        /* Native value for enumeration. */
        private final int code;

        Type(int code)
        {
            this.code = code;
        }

        /* Find Java object from native value. */
        protected static Type fromCode(int code)
        {
            for (Type t : Type.values())
            {
                if (t.code == code)
                {
                    return (t);
                }
            }

            return (null);
        }

        /* Get native value for Java object. */
        protected int toCode()
        {
            return (this.code);
        }
    }

    /* Instantiate new exception from type. */
    IBMatcherException(Type type)
    {
        this.type = type;
    }

    /**
     * Get type of exception.
     * 
     * @return type of exception
     */
    public Type getType()
    {
        return (this.type);
    }
}
