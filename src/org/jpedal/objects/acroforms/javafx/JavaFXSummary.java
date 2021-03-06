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
 * JavaFXSummary.java
 * ---------------
 */


package org.jpedal.objects.acroforms.javafx;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfObject;
import org.jpedal.utils.LogWriter;

public class JavaFXSummary extends javafx.scene.layout.Pane{

   private final Stage frame;
   private final PdfObject sigObject;
   private static TextField signedByBox = new TextField();
   private static TextField reasonBox = new TextField();
   private static TextField dateBox = new TextField();
   private static TextField locationBox = new TextField();
   


   public void setValues(final String signName, final String reason, final String location){
        signedByBox.setText(signName);
        reasonBox.setText(reason);

        final String rawDate = sigObject.getTextStreamValue(PdfDictionary.M);
        final StringBuilder date = new StringBuilder(rawDate);
           date.delete(0, 2);
           date.insert(4, '/');
           date.insert(7, '/');
           date.insert(10, ' ');
           date.insert(13, ':');
           date.insert(16, ':');
           date.insert(19, ' ');
           dateBox.setText(date.toString());

           locationBox.setText(location);
   }

   /**
    * Creates new form Signatures
    *
    * @param frame
    * @param sig
    */
   public JavaFXSummary(final Stage stage, final PdfObject sig) {
       this.frame = stage;
       this.sigObject = sig;
       initComponents();
   }

   /**
    * This method is called from within the constructor to
    * initialize the form.
    * WARNING: Do NOT modify this code. The content of this method is
    * always regenerated by the Form Editor.
    */
   // <editor-fold defaultstate="collapsed" desc=" Generated Code ">                          
    private void initComponents() {

        final Label locationLbl = new Label();
        final Label signedByLbl = new Label();
        final Label reasonLbl = new Label();
        final Label dateLbl = new Label();
        final Button showCertificateButton = new Button();
        final Button closeButton = new Button();
        locationBox = new TextField();
        signedByBox = new TextField();
        reasonBox = new TextField();
        dateBox = new TextField();

        locationLbl.setText("Location: ");
        signedByLbl.setText("Signed by: ");
        reasonLbl.setText("Reason: ");
        dateLbl.setText("Date: ");
        closeButton.setText("Close");
        showCertificateButton.setText("Show Certificate...");
        locationBox.setEditable(false);
        reasonBox.setEditable(false);
        dateBox.setEditable(false);
        signedByBox.setEditable(false);
        
        reasonBox.setPrefWidth(400);
        final byte[] bytes = sigObject.getTextStreamValueAsByte(PdfDictionary.Cert);
        if (bytes == null) {
            showCertificateButton.setDisable(true);
        } else {
            showCertificateButton.setOnAction(new EventHandler<javafx.event.ActionEvent>() {
                @Override
                public void handle(final ActionEvent t) {
                    showCertificate();
                }

            });
        }
        closeButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(final ActionEvent t) {
                frame.close();
            }
        });

        final VBox left = new VBox(20);
        left.getChildren().addAll(signedByLbl, reasonLbl, dateLbl, locationLbl);

        final VBox middle = new VBox(8);
        middle.getChildren().addAll(signedByBox, reasonBox, dateBox, locationBox);
        middle.setPadding(new Insets(3, 0, 3, 0));

        final HBox location = new HBox();
        location.getChildren().addAll(locationLbl, locationBox);
        locationBox.setAlignment(Pos.BOTTOM_CENTER);
        location.setAlignment(Pos.BOTTOM_CENTER);
        location.setPadding(new Insets(0, 0, 3, 0));

        final StackPane right = new StackPane();
        right.getChildren().addAll(showCertificateButton, closeButton);
        StackPane.setAlignment(closeButton, Pos.BOTTOM_RIGHT);
        StackPane.setAlignment(showCertificateButton, Pos.TOP_RIGHT);

        final HBox box = new HBox(3);
        box.getChildren().addAll(left, middle, location, right);

        this.getChildren().add(box);

    }

   private void showCertificate(){
       final Stage certificateStage = new Stage();
        final CertificateHolderFX ch = new CertificateHolderFX(certificateStage);
       try { 
           final byte[] bytes = sigObject.getTextStreamValueAsByte(PdfDictionary.Cert);
           final InputStream bais = new ByteArrayInputStream(bytes);
           final CertificateFactory cf = CertificateFactory.getInstance("X.509");
           final X509Certificate signingCertificate = (X509Certificate) cf.generateCertificate(bais);
           bais.close();

           final DateFormat format1 = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z");
           final Date notBefore = signingCertificate.getNotBefore();
           final Date notAfter = signingCertificate.getNotAfter();

           final String publicKey = byteToHex(signingCertificate.getPublicKey().getEncoded());
           final String x509Data = byteToHex(signingCertificate.getEncoded());
           final String sha1Digest = byteToHex(getDigest(bytes, "SHA1"));
           final String md5Digest = byteToHex(getDigest(bytes, "MD5"));

           String keyDescription = signingCertificate.getPublicKey().toString();
           final int keyDescriptionEnd = keyDescription.indexOf('\n');
           if (keyDescriptionEnd != -1) {
               keyDescription = keyDescription.substring(0, keyDescriptionEnd);
           }

            ch.setValues(sigObject.getTextStreamValue(PdfDictionary.Name), signingCertificate.getVersion(), signingCertificate.getSigAlgName(),
                    signingCertificate.getSubjectX500Principal().toString(),
                    signingCertificate.getIssuerX500Principal().toString(),
                    signingCertificate.getSerialNumber(),
                    format1.format(notBefore),
                    format1.format(notAfter),
                    keyDescription, publicKey, x509Data, sha1Digest, md5Digest);

       }catch(final Exception e){
            LogWriter.writeLog("Exception: " + e.getMessage());
       }


       }
       /**
    * @param bytes
    * @return
    * @throws NoSuchAlgorithmException
    */
   private static byte[] getDigest(final byte[] bytes, final String algorithm) throws NoSuchAlgorithmException {
       final MessageDigest sha1 = MessageDigest.getInstance(algorithm);
       sha1.update(bytes);
       return sha1.digest();
   }
   /**
    * @return
    */
   private static String byteToHex(final byte[] bytes) {
       StringBuilder hex = new StringBuilder();
       for (final byte aByte : bytes) {
           String singleByte = Integer.toHexString(aByte);
           if (singleByte.startsWith("ffffff")) {
               singleByte = singleByte.substring(6, singleByte.length());
           } else if (singleByte.length() == 1) {
               singleByte = '0' + singleByte;
           }

           singleByte = singleByte.toUpperCase();
           hex.append(singleByte).append(' ');
       }
       return hex.toString();
   }

}