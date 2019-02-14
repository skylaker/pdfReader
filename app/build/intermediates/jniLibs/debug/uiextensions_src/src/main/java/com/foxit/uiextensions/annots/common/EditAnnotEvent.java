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
package com.foxit.uiextensions.annots.common;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.uiextensions.annots.AnnotUndoItem;
import com.foxit.uiextensions.utils.Event;


public abstract class EditAnnotEvent extends Event {
    public static final int EVENTTYPE_ADD = 1;
    public static final int EVENTTYPE_MODIFY = 2;
    public static final int EVENTTYPE_DELETE = 3;

    public abstract boolean add();
    public abstract boolean modify();
    public abstract boolean delete();

    protected boolean execute() {
        if (mType == EVENTTYPE_ADD) {
            return add();
        } else if (mType == EVENTTYPE_MODIFY) {
            return modify();
        } else if (mType == EVENTTYPE_DELETE) {
            return delete();
        }

        return false;
    }


    public AnnotUndoItem mUndoItem;
    public Annot mAnnot;
    public PDFViewCtrl mPdfViewCtrl;
    public boolean useOldValue;

    public boolean isModifyDocument() {
        return true;
    }
}
