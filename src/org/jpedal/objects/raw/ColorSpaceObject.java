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
 * ColorSpaceObject.java
 * ---------------
 */
package org.jpedal.objects.raw;

import org.jpedal.color.ColorSpaces;
import org.jpedal.utils.LogWriter;

public class ColorSpaceObject extends PdfObject {

	//unknown CMAP as String
	//String unknownValue=null;
	
	String Name;
	
	byte[] rawName;
	
	PdfObject alternateSpace, IndexedColorSpace, Lookup, Process, tintTransform;
	
	int Alternate=PdfDictionary.Unknown, colorType=PdfDictionary.Unknown;

	private byte[][] rawComponents;
	
	private float[] BlackPoint, Gamma, WhitePoint;
	
	float GammaInCalGrey=-1, N=-1;
	
	private int hival=-1;

    public ColorSpaceObject(final String ref) {
        super(ref);
    }

    public ColorSpaceObject(final int ref, final int gen) {
       super(ref,gen);
    }

    public ColorSpaceObject(final byte[] bytes) {

        //set ID
    	colorType=PdfDictionary.getIntKey(0, bytes.length, bytes);
    	
	}

	@Override
    public byte[][] getStringArray(final int id) {

        switch(id){

            case PdfDictionary.Components:
       		    return deepCopy(rawComponents);

            default:
            	return super.getStringArray(id);
        }
    }

    @Override
    public void setStringArray(final int id, final byte[][] value) {

        switch(id){

            case PdfDictionary.Components:
            	rawComponents=value;
            break;

            default:
            	super.setStringArray(id, value);
        }

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

        case PdfDictionary.AlternateSpace:
        	return alternateSpace;
        	
        case PdfDictionary.Indexed:
        	return IndexedColorSpace;

        case PdfDictionary.Lookup:
        	return Lookup;

        case PdfDictionary.Process:
        	return Process;
        	
        case PdfDictionary.tintTransform:
            return tintTransform;

            default:
                return super.getDictionary(id);
        }
    }

    @Override
    public void setIntNumber(final int id, final int value){

        switch(id){

	        case PdfDictionary.hival:
	        	hival=value;
	        break;
//
//	        case PdfDictionary.Width:
//	            Width=value;
//	        break;

            default:
            	super.setIntNumber(id, value);
        }
    }

    @Override
    public int getInt(final int id){

        switch(id){

        	case PdfDictionary.hival:
            return hival;
//
//	        case PdfDictionary.Width:
//	            return Width;

            default:
            	return super.getInt(id);
        }
    }

    @Override
    public void setDictionary(final int id, final PdfObject value){

    	value.setID(id);
    	
        switch(id){

	        case PdfDictionary.AlternateSpace:
	        	alternateSpace=value;
	        	break;
        	
	        case PdfDictionary.Indexed:
	        	IndexedColorSpace=value;
	        	break;

            case PdfDictionary.Lookup:
	        	Lookup=value;
	        	break;
	        	
            case PdfDictionary.tintTransform:
            	tintTransform=value;
    		break;

            case PdfDictionary.Process:
            	Process=value;
    		break;

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

//            case PdfDictionary.Image:
//                PDFvalue =PdfDictionary.Image;
//            break;
//
//            case PdfDictionary.Form:
//                PDFvalue =PdfDictionary.Form;
//            break;

                case PdfDictionary.G:
                    PDFvalue =ColorSpaces.DeviceGray;
                break;


                case PdfDictionary.RGB:
                    PDFvalue =ColorSpaces.DeviceRGB;
                break;
                
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
        switch(pdfKeyType){

        case PdfDictionary.Alternate:
        	Alternate=PDFvalue;
    		break;
        	
        	case PdfDictionary.ColorSpace:
        		colorType=PDFvalue;
        		break;
        	
        }

        return PDFvalue;
    }
    
    @Override
    public int getParameterConstant(final int key) {
        final int def= PdfDictionary.Unknown;

        switch(key){

        case PdfDictionary.Alternate:
        	return Alternate;
        	
            case PdfDictionary.ColorSpace:
            	
                return colorType;


                default:
                	super.getParameterConstant(key);
        }

        return def;
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
            default:
            	super.setDoubleArray(id, value);
        }
    }

    /**
     * set values (access with getArrayIterator(int id))
     */
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

        	case PdfDictionary.BlackPoint:
        		return deepCopy(BlackPoint);

        	case PdfDictionary.Gamma:
        		return deepCopy(Gamma);
        		
        	case PdfDictionary.WhitePoint:
        		return deepCopy(WhitePoint);
        		
            default:
            	return super.getFloatArray(id);

        }
    }
    
    @Override
    public void setFloatNumber(final int id, final float value){

        switch(id){

        case PdfDictionary.Gamma:
        	GammaInCalGrey=value;
        	break;
        	
        	case PdfDictionary.N:
		        	N=value;
	    	break;
    	
            default:
            	super.setFloatNumber(id, value);
        }
    }
    
    @Override
    public float getFloatNumber(final int id){

        switch(id){

        case PdfDictionary.Gamma:
        	return GammaInCalGrey;
        	
	        case PdfDictionary.N:
	        	return N;
	        
            default:
            	return super.getFloatNumber(id);
        }
    }

    @Override
    public void setFloatArray(final int id, final float[] value) {

        switch(id){

	        case PdfDictionary.BlackPoint:
	            BlackPoint=value;
	        break;

	        case PdfDictionary.Gamma:
	            Gamma=value;
	        break;
	        
	        case PdfDictionary.WhitePoint:
	            WhitePoint=value;
	        break;
	        
            default:
            	super.setFloatArray(id, value);
        }
    }

    @Override
    public void setName(final int id, final byte[] value) {

        switch(id){


            case PdfDictionary.Name:
                rawName=value;
            break;

            default:
                super.setName(id,value);

        }

    }
    
    @Override
    public byte[] getStringValueAsByte(final int id) {

        switch(id){

            case PdfDictionary.Name:
                return rawName;

            default:
                return super.getStringValueAsByte(id);

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

            case PdfDictionary.Name:

            //setup first time
            if(Name==null && rawName!=null) {
                Name = new String(rawName);
            }

            return Name;

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
       // switch(id){

//            case PdfDictionary.BaseFont:
//                data=rawBaseFont;
//                break;

      //  }

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
    public int getObjectType() {
		return PdfDictionary.ColorSpace;
	}
    
    
    @Override
    public boolean decompressStreamWhenRead() {
		return true;
	}
}