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
package com.foxit.uiextensions.annots.fileattachment;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.FileSpec;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.FileAttachment;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.utils.AppDmUtil;

import java.io.File;
import java.util.Calendar;

public class FileAttachmentEvent extends EditAnnotEvent {

    public FileAttachmentEvent(int eventType, FileAttachmentUndoItem undoItem, FileAttachment highlight, PDFViewCtrl pdfViewCtrl) {
        mType = eventType;
        mUndoItem = undoItem;
        mAnnot = highlight;
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public boolean add() {
        if (mAnnot == null || !(mAnnot instanceof FileAttachment)) {
            return false;
        }
        FileAttachment annot = (FileAttachment) mAnnot;
        FileAttachmentUndoItem undoItem = (FileAttachmentUndoItem) mUndoItem;
        try {
            annot.setBorderColor(mUndoItem.mColor);
            annot.setOpacity(mUndoItem.mOpacity);
            if (mUndoItem.mContents != null) {
                annot.setContent(mUndoItem.mContents);
            }

            annot.setFlags(mUndoItem.mFlags);
            if (mUndoItem.mCreationDate != null) {
                annot.setCreationDateTime(mUndoItem.mCreationDate);
            }

            if (mUndoItem.mModifiedDate != null) {
                annot.setModifiedDateTime(mUndoItem.mModifiedDate);
            }

            if (mUndoItem.mAuthor != null) {
                annot.setTitle(mUndoItem.mAuthor);
            }

            annot.setIconName(undoItem.mIconName);
            annot.setFlags(mUndoItem.mFlags);
            annot.setUniqueID(mUndoItem.mNM);

            File file = new File(undoItem.mPath);
            FileSpec fileSpec = FileSpec.create(mPdfViewCtrl.getDoc());
            annot.setSubject("FileAttachment");
            if (undoItem.attacheName != null) {
                annot.setContent(undoItem.attacheName);
                fileSpec.setFileName(undoItem.attacheName);
            }

            fileSpec.embed(undoItem.mPath);

            Calendar cal = Calendar.getInstance();
            long time = file.lastModified();
            cal.setTimeInMillis(time);
            fileSpec.setModifiedDateTime(AppDmUtil.javaDateToDocumentDate(time));
            annot.setFileSpec(fileSpec);
            annot.resetAppearanceStream();
            Module module = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_FILEATTACHMENT);
            FileAttachmentToolHandler toolHandler = ((FileAttachmentToolHandler)((FileAttachmentModule)module).getToolHandler());
            if(toolHandler!= null)
                toolHandler.setAttachmentPath(annot,undoItem.mPath);

            return  true;
        } catch (PDFException e) {
            e.printStackTrace();
            if (e.getLastError() == PDFException.e_errOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
        }
        return false;
    }

    @Override
    public boolean modify() {
        if (mAnnot == null || !(mAnnot instanceof FileAttachment)) {
            return false;
        }
        FileAttachment annot = (FileAttachment) mAnnot;
        try {
            if (mUndoItem.mModifiedDate != null) {
                annot.setModifiedDateTime(mUndoItem.mModifiedDate);
            }
            if (mUndoItem.mContents == null) {
                mUndoItem.mContents = "";
            }
            annot.setBorderColor(mUndoItem.mColor);
            annot.setOpacity(mUndoItem.mOpacity);
            annot.setIconName(((FileAttachmentModifyUndoItem) mUndoItem).mIconName);
            annot.move(mUndoItem.mBBox);
            annot.resetAppearanceStream();
            return true;
        } catch (PDFException e) {
            if (e.getLastError() == PDFException.e_errOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
        }
        return false;
    }

    @Override
    public boolean delete() {
        if (mAnnot == null || !(mAnnot instanceof FileAttachment)) {
            return false;
        }

        try {

            PDFPage page = mAnnot.getPage();
            page.removeAnnot(mAnnot);
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

}

