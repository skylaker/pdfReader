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
package com.foxit.uiextensions;


public interface Module {
    public static final String MODULE_NAME_PDFREADER = "PDFReader Module";
    public static final String MODULE_NAME_LOCAL = "Local Module";
    //Annot module
    public static final String MODULE_NAME_DEFAULT = "Default";
    public static final String MODULE_NAME_NOTE = "Note Module";
    public static final String MODULE_NAME_HIGHLIGHT = "Highlight Module";
    public static final String MODULE_NAME_UNDERLINE = "Underline Module";
    public static final String MODULE_NAME_STRIKEOUT = "Strikeout Module";
    public static final String MODULE_NAME_SQUIGGLY = "Squiggly Module";
    public static final String MODULE_NAME_LINK = "Link Module";
    public static final String MODULE_NAME_CIRCLE = "Circle Module";
    public static final String MODULE_NAME_SQUARE = "Rectangle Module";
    public static final String MODULE_NAME_TYPEWRITER = "Typewriter Module";
    public static final String MODULE_NAME_CARET = "Caret Module";
    public static final String MODULE_NAME_INK = "Ink Module";
    public static final String MODULE_NAME_ERASER = "Eraser Module";
    public static final String MODULE_NAME_STAMP = "Stamp Module";
    public static final String MODULE_NAME_LINE = "Line Module";
    public static final String MODULE_NAME_FILEATTACHMENT = "FileAttachment Module";
    public static final String MODULE_NAME_FILE_PANEL = "FileAttachment List Module";
    public static final String MODULE_NAME_DIGITALSIGNATURE = "Digital Signature Module";
    public static final String MODULE_NAME_PSISIGNATURE = "PSI Signature Module";
    public static final String MODULE_NAME_FORMFILLER = "FormFiller Module";
    public static final String MODULE_NAME_FORM_NAVIGATION = "Navigation Module";
    //Serarch module
    public static final String MODULE_NAME_SEARCH = "Search Module";

    //
    public static final String MODULE_MORE_MENU = "More Menu Module";

    // Docuemnt Information
    public static final String MODULE_NAME_DOCINFO = "DocumentInfo Module";

    public static final String MODULE_NAME_SELECTION = "TextSelect Module";
    public static final String MODULE_NAME_REPLY= "Reply Module";

    public static final String MODULE_NAME_ANNOTPANEL = "Annotations Module";
    public static final String MODULE_NAME_BOOKMARK = "Bookmark Module";
    public static final String MODULE_NAME_OUTLINE = "Outline Module";

    public static final String MODULE_NAME_PAGENAV = "Page Navigation Module";

    public static final String MODULE_NAME_THUMBNAIL = "Thumbnail Module";

    public static final String MODULE_NAME_BRIGHTNESS = "Brightness Module";

    public static final String MODULE_NAME_REFLOW = "Reflow Module";
    //undo redo module
    public static final String MODULE_NAME_UNDO = "Undo Redo Module";

    public static final String MODULE_NAME_PASSWORD = "Password Module";


    // crop module
    public static final String MODULE_NAME_CROP = "Crop Module";
    String getName();

    boolean loadModule();

    boolean unloadModule();
}
