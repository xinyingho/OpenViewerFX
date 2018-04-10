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
 * CIDFontType2.java
 * ---------------
 */
package org.jpedal.fonts;

import java.util.Map;

import org.jpedal.fonts.tt.TTGlyphs;
import org.jpedal.io.PdfObjectReader;
import org.jpedal.io.ObjectStore;
import org.jpedal.parser.PdfFontFactory;
import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfObject;


/**
 * handles truetype specifics
 *  */
public class CIDFontType2 extends TrueType {
    
    /**get handles onto Reader so we can access the file*/
    public CIDFontType2(final PdfObjectReader currentPdfFile, final String substituteFontFile) {
        
        isCIDFont=true;
        TTstreamisCID=true;
        
        glyphs=new TTGlyphs();
        
        init(currentPdfFile);
        
        this.substituteFontFile=substituteFontFile;
        
    }
    
    /**get handles onto Reader so we can access the file*/
    public CIDFontType2(final PdfObjectReader currentPdfFile, final boolean ttflag) {
        
        isCIDFont=true;
        TTstreamisCID=ttflag;
        
        glyphs=new TTGlyphs();
        
        init(currentPdfFile);
        
    }
    
    /**read in a font and its details from the pdf file*/
    @Override
    public void createFont(final PdfObject pdfObject, final String fontID, final boolean renderPage, final ObjectStore objectStore, final Map substitutedFonts) throws Exception{
        
        fontTypes=StandardFonts.CIDTYPE2;
        this.fontID=fontID;
        
        final PdfObject Descendent=pdfObject.getDictionary(PdfDictionary.DescendantFonts);
        final PdfObject pdfFontDescriptor=Descendent.getDictionary(PdfDictionary.FontDescriptor);
        
        createCIDFont(pdfObject,Descendent);
        
        if (pdfFontDescriptor!= null) {
            
            final byte[] stream;
            final PdfObject FontFile2=pdfFontDescriptor.getDictionary(PdfDictionary.FontFile2);
            if(FontFile2!=null){
                stream=currentPdfFile.readStream(FontFile2,true,true,false, false,false, FontFile2.getCacheName(currentPdfFile.getObjectReader()));
                
                if(stream!=null) {
                    readEmbeddedFont(stream,null,hasEncoding);
                }
            }
        }
        
        
        //allow for corrupted
        final boolean isCorrupt=glyphs.isCorrupted();
        
        if(glyphs.isCorrupted()){
            
            final PdfFontFactory pdfFontFactory =new PdfFontFactory(currentPdfFile);
            pdfFontFactory.getFontSub(getBaseFontName());
            isFontEmbedded=false;
            
            substituteFontFile= pdfFontFactory.getMapFont();
            
            if(substituteFontFile==null){
                
                glyphs.setFontEmbedded(false);
                
                //generic setup so we have Java fonts available
                init(fontID, renderPage);
                
            }
        }
        
        //setup and substitute font
        if(renderPage && !isFontEmbedded && substituteFontFile!=null){
            this.substituteFontUsed(substituteFontFile);
            isFontSubstituted=true;
            this.isFontEmbedded=true;
            
            glyphs.setFontEmbedded(true);
        }
        
        //make sure a font set
        if (renderPage) {
            setFont(getFontName(), 1);
        }

        glyphs.setCorrupted(isCorrupt);
        
    }
}
