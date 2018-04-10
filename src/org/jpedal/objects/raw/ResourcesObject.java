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
 * ResourcesObject.java
 * ---------------
 */
package org.jpedal.objects.raw;

import org.jpedal.utils.LogWriter;

public class ResourcesObject extends PdfObject {

	//unknown CMAP as String 
	//String unknownValue=null;
	
	private byte[][] ProcSet;
	
	private PdfObject ExtGState,Font, Pattern,XObject, Properties;
	
    public ResourcesObject(final String ref) {
        super(ref);
    }

    public ResourcesObject(final int ref, final int gen) {
       super(ref,gen);
    }


    @Override
    public PdfObject getDictionary(final int id){

        switch(id){

            case PdfDictionary.ExtGState:
                return ExtGState;
                
            case PdfDictionary.Font:
                return Font;

            case PdfDictionary.Pattern:
        		return Pattern;

            case PdfDictionary.Properties:
                return Properties;

            case PdfDictionary.XObject:
                return XObject;

            default:
                return super.getDictionary(id);
        }
    }

    @Override
    public void setIntNumber(final int id, final int value){

        switch(id){

//	        case PdfDictionary.DW:
//	            DW=value;
//	        break;
        
            default:
            	super.setIntNumber(id, value);
        }
    }

    @Override
    public int getInt(final int id){

        switch(id){

//	        case PdfDictionary.DW:
//	            return DW;
	          
            default:
            	return super.getInt(id);
        }
    }

    @Override
    public void setDictionary(final int id, final PdfObject value){

    	value.setID(id);
    	
        switch(id){

            case PdfDictionary.ExtGState:
            	ExtGState=value;
    		break;
    		
            case PdfDictionary.Font:
            	Font=value;
    		break;

            case PdfDictionary.Pattern:
                Pattern=value;
            break;

            case PdfDictionary.Properties:
                Properties=value;
			break;
			
            case PdfDictionary.XObject:
            	XObject=value;
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
   //     switch(pdfKeyType){

//        	case PdfDictionary.BaseEncoding:
//        		BaseEncoding=PDFvalue;
//        		break;
        
    //    }
        
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
        
      //check general values
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

            case PdfDictionary.ProcSet:
                return new PdfArrayIterator(ProcSet);

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

            case PdfDictionary.ProcSet:
            	ProcSet=value;
            break;
            
            default:
            	super.setMixedArray(id, value);
        }

    }

    @Override
    public float[] getFloatArray(final int id) {

        switch(id){
            default:
            	return super.getFloatArray(id);
        }
    }

    @Override
    public void setFloatArray(final int id, final float[] value) {

        switch(id){

//	        case PdfDictionary.FontBBox:
//	            FontBBox=value;
//	        break;
        
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

    /**
     * unless you need special fucntions,
     * use getStringValue(int id) which is faster
     */
    @Override
    public String getStringValue(final int id, final int mode) {

        final byte[] data=null;

        //get data
   //     switch(id){

//            case PdfDictionary.BaseFont:
//                data=rawBaseFont;
//                break;
            
  //      }

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
		return PdfDictionary.Resources;
	}
}
