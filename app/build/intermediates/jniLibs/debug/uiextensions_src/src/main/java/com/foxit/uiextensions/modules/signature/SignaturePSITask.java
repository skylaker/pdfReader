/**
 * Copyright (C) 2003-2017, Foxit Software Inc..
 * All Rights Reserved.
 * <p>
 * http://www.foxitsoftware.com
 * <p>
 * The following code is copyrighted and is the proprietary of Foxit Software Inc.. It is not allowed to
 * distribute any parts of Foxit Mobile PDF SDK to third party or public without permission unless an agreement
 * is signed between Foxit Software Inc. and customers to explicitly grant customers permissions.
 * Review legal.txt for additional license and legal information.
 */
package com.foxit.uiextensions.modules.signature;

import android.graphics.Bitmap;
import android.graphics.Matrix;

import com.foxit.sdk.Task;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.graphics.PDFGraphicsObject;
import com.foxit.sdk.pdf.graphics.PDFImageObject;
import com.foxit.sdk.pdf.psi.PSI;
import com.foxit.uiextensions.utils.Event;


public class SignaturePSITask extends Task {

    private SignatureEvent mEvent;
    protected static PSI mPsi;
    private static final int INK_DIAMETER_SCALE = 10;

    public SignaturePSITask(final SignatureEvent event, final Event.Callback callBack) {
        super(new CallBack() {
            @Override
            public void result(Task task) {
                if(event instanceof SignatureDrawEvent) {
                    if(((SignatureDrawEvent) event).mCallBack!=null)
                        ((SignatureDrawEvent) event).mCallBack.result(event,true);

                }else if(event instanceof SignatureSignEvent){

                    if(((SignatureSignEvent) event).mCallBack!=null)
                        ((SignatureSignEvent) event).mCallBack.result(event,true);
                }

                if(callBack!= null)
                    callBack.result(event, true);

            }
        });
        mEvent= event;
    }

    public Bitmap getBitmap()
    {

        try {
            if(mPsi!= null)
                return mPsi.getBitmap();
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void execute() {

        if(mEvent instanceof SignatureDrawEvent) {
            try {
            if(((SignatureDrawEvent) mEvent).mType == SignatureConstants.SG_EVENT_DRAW)
            {
                //Currently the demo turns off simulation of Pressure Sensitive Ink
                mPsi = PSI.create(((SignatureDrawEvent) mEvent).mBitmap, false);
                mPsi.setColor(((SignatureDrawEvent) mEvent).mColor);
                mPsi.setDiameter((int) ((SignatureDrawEvent) mEvent).mThickness * INK_DIAMETER_SCALE);
                mPsi.setOpacity(1f);
            }
            else if(((SignatureDrawEvent) mEvent).mType == SignatureConstants.SG_EVENT_THICKNESS)
                mPsi.setDiameter((int) ((SignatureDrawEvent) mEvent).mThickness * INK_DIAMETER_SCALE);
            else if(((SignatureDrawEvent) mEvent).mType == SignatureConstants.SG_EVENT_COLOR) {
                mPsi.setColor((int) ((SignatureDrawEvent) mEvent).mColor);
            }

            } catch (PDFException e) {
                e.printStackTrace();
            }

        }
        else if(mEvent instanceof SignatureSignEvent)
        {
            try {
                PDFImageObject imageObject = PDFImageObject.create(((SignatureSignEvent) mEvent).mPage.getDocument());
                imageObject.setBitmap(((SignatureSignEvent) mEvent).mBitmap, null);

                Matrix matrix = new Matrix();
                float width = ((SignatureSignEvent) mEvent).mRect.width();
                float height = ((SignatureSignEvent) mEvent).mRect.height();
                matrix.setScale(Math.abs(width), Math.abs(height));
                matrix.postTranslate(((SignatureSignEvent) mEvent).mRect.left, ((SignatureSignEvent) mEvent).mRect.bottom);
                imageObject.setMatrix(matrix);
                long pos = ((SignatureSignEvent) mEvent).mPage.getLastGraphicsObjectPosition(PDFGraphicsObject.e_graphicsObjTypeAll);
                ((SignatureSignEvent) mEvent).mPage.insertGraphicsObject(pos,imageObject);
                ((SignatureSignEvent) mEvent).mPage.generateContent();
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }
    }
}
