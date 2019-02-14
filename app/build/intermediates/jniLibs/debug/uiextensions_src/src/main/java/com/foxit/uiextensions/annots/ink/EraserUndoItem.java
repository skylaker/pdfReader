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
package com.foxit.uiextensions.annots.ink;

import com.foxit.uiextensions.annots.AnnotUndoItem;

import java.util.ArrayList;

public class EraserUndoItem extends AnnotUndoItem {
    private ArrayList<InkUndoItem> undoItems;

    public EraserUndoItem(){
        undoItems = new ArrayList<InkUndoItem>();
    }

    public void addUndoItem(InkUndoItem undoItem){
        undoItems.add(undoItem);
    }

    @Override
    public boolean undo() {
        int size = undoItems.size();
        for (int i = size - 1; i >= 0; i--) {
            InkUndoItem undoItem = undoItems.get(i);
            if (undoItem instanceof InkModifyUndoItem) {
                InkModifyUndoItem item = (InkModifyUndoItem) undoItem;
                item.undo();
            } else if (undoItem instanceof InkDeleteUndoItem){
                InkDeleteUndoItem item = (InkDeleteUndoItem) undoItem;
                item.undo();
            }
        }
        return false;
    }

    @Override
    public boolean redo() {
        for (InkUndoItem undoItem : undoItems) {
            if (undoItem instanceof InkModifyUndoItem) {
                InkModifyUndoItem item = (InkModifyUndoItem) undoItem;
                item.redo();
            } else if (undoItem instanceof InkDeleteUndoItem){
                InkDeleteUndoItem item = (InkDeleteUndoItem) undoItem;
                item.redo();
            }
        }
        return false;
    }
}
