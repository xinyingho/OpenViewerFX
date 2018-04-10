/*
 * ===========================================
 * Java Pdf Extraction Decoding Access Library
 * ===========================================
 *
 * Project Info:  http://www.idrsolutions.com
 * Help section for developers at http://www.idrsolutions.com/support/
 *
 * (C) Copyright 1997-2015 IDRsolutions and Contributors.
 *
 * This file is part of JPedal/JPDF2HTML5
 *
     This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA


 *
 * ---------------
 * FunctionObject.java
 * ---------------
 */
package org.jpedal.objects.raw;

import org.jpedal.utils.LogWriter;

public class FunctionObject extends PdfObject {

	//unknown CMAP as String
	//String unknownValue=null;

    byte[][] Functions;

    float[] Bounds, C0,C1, Encode;

    int[] Size;

    int BitsPerSample=-1, FunctionType=-1;
    
    float N=-1;
    
    public FunctionObject(final String ref) {
        super(ref);
    }

    public FunctionObject(final int ref, final int gen) {
       super(ref,gen);
    }
    
    @Override
    public int[] getIntArray(final int id) {

    	switch(id){

	    	case PdfDictionary.Size:
	    		return deepCopy(Size);
    		
            default:
            	return super.getIntArray(id);
        }
    }

    @Override
    public void setIntArray(final int id, final int[] value) {

    	switch(id){

    	case PdfDictionary.Size:
    		Size=value;
    		break;
    		
            default:
            	super.setIntArray(id, value);
        }
    }


    public FunctionObject(final int type) {
    	super(type);
	}

    @Override
    public boolean getBoolean(final int id){

        switch(id){

//        case PdfDictionary.ImageMask:
//        	return ImageMask;
//

            default:
            	return super.getBoolean(id);
        }

    }

    @Override
    public byte[][] getKeyArray(final int id) {

        switch(id){

            case PdfDictionary.Functions:
       		    return deepCopy(Functions);

            default:
            	return super.getKeyArray(id);
        }
    }

    @Override
    public void setKeyArray(final int id, final byte[][] value) {

        switch(id){

            case PdfDictionary.Functions:
                Functions=value;
            break;

            default:
            	super.setKeyArray(id, value);
        }

    }

    @Override
    public void setBoolean(final int id, final boolean value){

        switch(id){

//        case PdfDictionary.ImageMask:
//        	ImageMask=value;
//        	break;

            default:
                super.setBoolean(id, value);
        }
    }

    @Override
    public PdfObject getDictionary(final int id){

        switch(id){

//            case PdfDictionary.XObject:
//                return XObject;

            default:
                return super.getDictionary(id);
        }
    }

    @Override
    public void setIntNumber(final int id, final int value){

        switch(id){
        
        case PdfDictionary.BitsPerSample:
        	BitsPerSample=value;
        	break;

	        case PdfDictionary.FunctionType:
	            FunctionType=value;
	        break;

	        case PdfDictionary.N:
	            N=value;
	        break;

            default:
            	super.setIntNumber(id, value);
        }
    }

    @Override
    public int getInt(final int id){

        switch(id){

        case PdfDictionary.BitsPerSample:
        	return BitsPerSample;
        
	        case PdfDictionary.FunctionType:
	            return FunctionType;
	
            default:
            	return super.getInt(id);
        }
    }
    
    @Override
    public float getFloatNumber(final int id){

        switch(id){

	        case PdfDictionary.N:
	        	return N;
	        
            default:
            	return super.getFloatNumber(id);
        }
    }

    @Override
    public void setFloatNumber(final int id, final float value){

        switch(id){

        	case PdfDictionary.N:
		        	N=value;
	    	break;
    	
            default:
            	super.setFloatNumber(id, value);
        }
    }


    @Override
    public void setDictionary(final int id, final PdfObject value){

    	value.setID(id);
    	
        switch(id){

//            case PdfDictionary.XObject:
//            	XObject=value;
//    		break;

            default:
            	super.setDictionary(id, value);
        }
    }


    @Override
    public int setConstant(final int pdfKeyType, final int keyStart, final int keyLength, final byte[] raw) {

        int PDFvalue =PdfDictionary.Unknown;

        int id=0,x=0,next;

        try{

            //convert token to unique key which we can lookup

            for(int i2=keyLength-1;i2>-1;i2--){

            	next=raw[keyStart+i2];

            	next -= 48;

                id += ((next)<<x);

                x += 8;
            }

            switch(id){


//            case PdfDictionary.Form:
//                PDFvalue =PdfDictionary.Form;
//            break;

//                case StandardFonts.CIDTYPE0:
//                    PDFvalue =StandardFonts.CIDTYPE0;
//                break;


                default:

//                	if(pdfKeyType==PdfDictionary.Encoding){
//                		PDFvalue=PdfCIDEncodings.getConstant(id);
//
//                		if(PDFvalue==PdfDictionary.Unknown){
//
//                			byte[] bytes=new byte[keyLength];
//
//                            System.arraycopy(raw,keyStart,bytes,0,keyLength);
//
//                			unknownValue=new String(bytes);
//                		}
//
//                		if(debug && PDFvalue==PdfDictionary.Unknown){
//                			System.out.println("Value not in PdfCIDEncodings");
//
//                           	 byte[] bytes=new byte[keyLength];
//
//                               System.arraycopy(raw,keyStart,bytes,0,keyLength);
//                               System.out.println("Add to CIDEncodings and as String");
//                               System.out.println("key="+new String(bytes)+" "+id+" not implemented in setConstant in PdfFont Object");
//
//                               System.out.println("final public static int CMAP_"+new String(bytes)+"="+id+";");
//                               
//                		}
//                	}else
                		PDFvalue=super.setConstant(pdfKeyType,id);

                    if(PDFvalue==-1 && debug){

                        	 final byte[] bytes=new byte[keyLength];

                            System.arraycopy(raw,keyStart,bytes,0,keyLength);
                            System.out.println("key="+new String(bytes)+ ' ' +id+" not implemented in setConstant in "+this);

                            System.out.println("final public static int "+new String(bytes)+ '=' +id+ ';');
                            
                        }

                    break;

            }

        }catch(final Exception e){
            LogWriter.writeLog("Exception: " + e.getMessage());
        }

        //System.out.println(pdfKeyType+"="+PDFvalue);
    //    switch(pdfKeyType){
//
//        	case PdfDictionary.Subtype:
//        		subtype=PDFvalue;
//        		break;
//
   //     }

        return PDFvalue;
    }

    @Override
    public int getParameterConstant(final int key) {

        switch(key){


//            case PdfDictionary.BaseEncoding:
//
//            	//special cases first
//            	if(key==PdfDictionary.BaseEncoding && Encoding!=null && Encoding.isZapfDingbats)
//            		return StandardFonts.ZAPF;
//            	else if(key==PdfDictionary.BaseEncoding && Encoding!=null && Encoding.isSymbol)
//            		return StandardFonts.SYMBOL;
//            	else
//            		return BaseEncoding;
        default:
        	return super.getParameterConstant(key);

        }
    }

//    public void setStream(){
//
//        hasStream=true;
//    }


    @Override
    public PdfArrayIterator getMixedArray(final int id) {

    	switch(id){

            //case PdfDictionary.Differences:
            //    return new PdfArrayIterator(Differences);

            default:
			return super.getMixedArray(id);
        }
	}

    @Override
    public double[] getDoubleArray(final int id) {

        switch(id){
            default:
            	return super.getDoubleArray(id);
        }
    }

    @Override
    public void setDoubleArray(final int id, final double[] value) {

        switch(id){

//            case PdfDictionary.FontMatrix:
//                FontMatrix=value;
//            break;

            default:
            	super.setDoubleArray(id, value);
        }
    }

    @Override
    public void setMixedArray(final int id, final byte[][] value) {

        switch(id){

//            case PdfDictionary.Differences:
//                Differences=value;
//            break;

            default:
            	super.setMixedArray(id, value);
        }
    }

    @Override
    public float[] getFloatArray(final int id) {

        switch(id){

        	case PdfDictionary.Bounds:
        		return deepCopy(Bounds);

            case PdfDictionary.C0:
        		return deepCopy(C0);

            case PdfDictionary.C1:
        		return deepCopy(C1);

            case PdfDictionary.Encode:
        		return deepCopy(Encode);

            default:
            	return super.getFloatArray(id);

        }
    }

    @Override
    public void setFloatArray(final int id, final float[] value) {

        switch(id){

	        case PdfDictionary.Bounds:
	            Bounds=value;
	        break;

            case PdfDictionary.C0:
	            C0=value;
	        break;

            case PdfDictionary.C1:
	            C1=value;
	        break;

            case PdfDictionary.Encode:
	            Encode=value;
	        break;

            default:
            	super.setFloatArray(id, value);
        }
    }

    @Override
    public void setName(final int id, final byte[] value) {

        switch(id){


//            case PdfDictionary.CMapName:
//                rawCMapName=value;
//            break;

            default:
                super.setName(id,value);

        }

    }

    @Override
    public void setTextStreamValue(final int id, final byte[] value) {

        switch(id){

//	        case PdfDictionary.CharSet:
//	            rawCharSet=value;
//	        break;
//

            default:
                super.setTextStreamValue(id,value);

        }

    }

    @Override
    public String getName(final int id) {

        switch(id){

//            case PdfDictionary.BaseFont:
//
//            //setup first time
//            if(BaseFont==null && rawBaseFont!=null)
//                BaseFont=new String(rawBaseFont);
//
//            return BaseFont;

            default:
                return super.getName(id);

        }
    }

    @Override
    public String getTextStreamValue(final int id) {

        switch(id){

//	        case PdfDictionary.CharSet:
//
//	            //setup first time
//	            if(CharSet==null && rawCharSet!=null)
//	            	CharSet=new String(rawCharSet);
//
//	            return CharSet;

            default:
                return super.getTextStreamValue(id);

        }
    }

    /**
     * unless you need special fucntions,
     * use getStringValue(int id) which is faster
     */
    @Override
    public String getStringValue(final int id, final int mode) {

        final byte[] data=null;

        //get data
    //    switch(id){

//            case PdfDictionary.BaseFont:
//                data=rawBaseFont;
//                break;

    //    }

        //convert
        switch(mode){
            case PdfDictionary.STANDARD:

                //setup first time
                if(data!=null) {
                    return new String(data);
                } else {
                    return null;
                }


            case PdfDictionary.LOWERCASE:

                //setup first time
                if(data!=null) {
                    return new String(data);
                } else {
                    return null;
                }

            case PdfDictionary.REMOVEPOSTSCRIPTPREFIX:

                //setup first time
                if(data!=null){
                	final int len=data.length;
                	if(len>6 && data[6]=='+'){ //lose ABCDEF+ if present
                		final int length=len-7;
                		final byte[] newData=new byte[length];
                		System.arraycopy(data, 7, newData, 0, length);
                		return new String(newData);
                	}else {
                        return new String(data);
                    }
                }else {
                    return null;
                }

            default:
                throw new RuntimeException("Value not defined in getName(int,mode) in "+this);
        }
    }
    
    @Override
    public boolean decompressStreamWhenRead() {
		return true;
	}
    
    @Override
    public int getObjectType() {
		return PdfDictionary.Function;
	}
}