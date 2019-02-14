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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.support.annotation.NonNull;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewGroup;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFError;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.caret.CaretModule;
import com.foxit.uiextensions.annots.circle.CircleModule;
import com.foxit.uiextensions.annots.fileattachment.FileAttachmentModule;
import com.foxit.uiextensions.annots.form.FormFillerModule;
import com.foxit.uiextensions.annots.form.FormNavigationModule;
import com.foxit.uiextensions.annots.freetext.typewriter.TypewriterModule;
import com.foxit.uiextensions.annots.ink.EraserModule;
import com.foxit.uiextensions.annots.ink.InkModule;
import com.foxit.uiextensions.annots.line.LineModule;
import com.foxit.uiextensions.annots.link.LinkModule;
import com.foxit.uiextensions.annots.note.NoteModule;
import com.foxit.uiextensions.annots.square.SquareModule;
import com.foxit.uiextensions.annots.stamp.StampModule;
import com.foxit.uiextensions.annots.textmarkup.highlight.HighlightModule;
import com.foxit.uiextensions.annots.textmarkup.squiggly.SquigglyModule;
import com.foxit.uiextensions.annots.textmarkup.strikeout.StrikeoutModule;
import com.foxit.uiextensions.annots.textmarkup.underline.UnderlineModule;
import com.foxit.uiextensions.controls.menu.MoreMenuModule;
import com.foxit.uiextensions.home.local.LocalModule;
import com.foxit.uiextensions.modules.BrightnessModule;
import com.foxit.uiextensions.modules.DocInfoModule;
import com.foxit.uiextensions.modules.OutlineModule;
import com.foxit.uiextensions.modules.PageNavigationModule;
import com.foxit.uiextensions.modules.ReadingBookmarkModule;
import com.foxit.uiextensions.modules.ReflowModule;
import com.foxit.uiextensions.modules.ScreenLockModule;
import com.foxit.uiextensions.modules.SearchModule;
import com.foxit.uiextensions.modules.UndoModule;
import com.foxit.uiextensions.modules.crop.CropModule;
import com.foxit.uiextensions.modules.panel.IPanelManager;
import com.foxit.uiextensions.modules.panel.PanelManager;
import com.foxit.uiextensions.modules.panel.annot.AnnotPanelModule;
import com.foxit.uiextensions.modules.panel.filespec.FileSpecPanelModule;
import com.foxit.uiextensions.modules.signature.SignatureModule;
import com.foxit.uiextensions.modules.thumbnail.ThumbnailModule;
import com.foxit.uiextensions.pdfreader.IPDFReader;
import com.foxit.uiextensions.pdfreader.impl.PDFReader;
import com.foxit.uiextensions.security.digitalsignature.DigitalSignatureModule;
import com.foxit.uiextensions.security.standard.PasswordModule;
import com.foxit.uiextensions.textselect.BlankSelectToolHandler;
import com.foxit.uiextensions.textselect.TextSelectModule;
import com.foxit.uiextensions.textselect.TextSelectToolHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Class <CODE>UIExtensionsManager</CODE> represents a UI extensions manager.
 * <p/>
 * The <CODE>UIExtensionsManager</CODE> class is mainly used for manage the UI extensions which implement {@link ToolHandler} interface, it implements the {@link PDFViewCtrl.UIExtensionsManager}
 * interface that is a listener to listen common interaction events and view event, and will dispatch some events to UI extensions, it also defines functions to manage the UI extensions.
 */
public class UIExtensionsManager implements PDFViewCtrl.UIExtensionsManager {

    /**
     * The interface of {@link ToolHandler} change listener.
     */
    public interface ToolHandlerChangedListener {
        /**
         * Called when current {@link ToolHandler} is changed.
         *
         * @param oldToolHandler The old tool handler.
         * @param newToolHandler The new tool handler.
         */
        void onToolHandlerChanged(ToolHandler oldToolHandler, ToolHandler newToolHandler);
    }

    public interface ConfigurationChangedListener {
        /**
         * Called when {@link UIExtensionsManager#onConfigurationChanged(Configuration)} is called.
         *
         * @param newConfig
         */
        void onConfigurationChanged(Configuration newConfig);
    }

    public interface MenuEventListener {
        /**
         * Called when {@link #triggerDismissMenuEvent()} is called.
         */
        void onTriggerDismissMenu();
    }

    private ToolHandler mCurToolHandler = null;
    private PDFViewCtrl mPdfViewCtrl = null;
    private List<Module> mModules = new ArrayList<Module>();
    private HashMap<String, ToolHandler> mToolHandlerList;
    private SparseArray<AnnotHandler> mAnnotHandlerList;
    private ArrayList<ToolHandlerChangedListener> mHandlerChangedListeners;
    private ArrayList<ConfigurationChangedListener> mConfigurationChangedListeners;
    private ArrayList<MenuEventListener> mMenuEventListeners;

    private Activity mAttachActivity = null;
    private Context mContext;
    private ViewGroup mParent;
    private Config mModulesConfig;
    private PDFReader mPDFReader;
    private boolean mEnableLinkAnnot = true;
    private boolean mEnableLinkHighlight = true;
    private boolean mEnableFormHighlight = true;
    private long mFormHighlightColor = 0x200066cc;
    private long mLinkHighlightColor = 0x16007FFF;
    private int mSelectHighlightColor = 0xFFACDAED;
    private IPanelManager mPanelManager = null;

    /**
     * Instantiates a new UI extensions manager.
     *
     * @param context     A <CODE>Context</CODE> object which species the context.
     * @param parent      An <CODE>ViewGroup</CODE> object which species the parent layout of <CODE>pdfViewCtrl</CODE> object.
     *                    It can be null while use the default reader framework.
     * @param pdfViewCtrl A <CODE>PDFViewCtrl</CODE> object which species the PDF view control.
     */
    public UIExtensionsManager(Context context, ViewGroup parent, final PDFViewCtrl pdfViewCtrl) {
        if (pdfViewCtrl == null) {
            throw new NullPointerException("PDF view control can't be null");
        }

        init(context, parent, pdfViewCtrl, null);
    }

    /**
     * Instantiates a new UI extensions manager with modules config.
     *
     * @param context     A <CODE>Context</CODE> object which species the context.
     * @param parent      An <CODE>ViewGroup</CODE> object which species the parent layout of <CODE>pdfViewCtrl</CODE> object.
     *                    It can be null while use the default reader framework.
     * @param pdfViewCtrl A <CODE>PDFViewCtrl</CODE> object which species the PDF view control.
     * @param config      A <CODE>Config</CODE> object which species a modules loading config,
     *                    if null, UIExtension manager will load all modules by default, and equal to {@link #UIExtensionsManager(Context, ViewGroup, PDFViewCtrl)}.
     */
    public UIExtensionsManager(Context context, ViewGroup parent, final PDFViewCtrl pdfViewCtrl, Config config) {
        if (pdfViewCtrl == null) {
            throw new NullPointerException("PDF view control can't be null");
        }
        init(context, parent, pdfViewCtrl, config);
    }

    private void init(Context context, ViewGroup parent, final PDFViewCtrl pdfViewCtrl, Config config) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;

        mToolHandlerList = new HashMap<String, ToolHandler>(8);
        mAnnotHandlerList = new SparseArray<AnnotHandler>(8);
        mHandlerChangedListeners = new ArrayList<ToolHandlerChangedListener>();
        mConfigurationChangedListeners = new ArrayList<ConfigurationChangedListener>();
        mMenuEventListeners = new ArrayList<MenuEventListener>();
        pdfViewCtrl.registerDocEventListener(mDocEventListener);
        pdfViewCtrl.registerRecoveryEventListener(mRecoveryEventListener);
        pdfViewCtrl.registerDoubleTapEventListener(mDoubleTapEventListener);
        registerMenuEventListener(mMenuEventListener);

        if (config == null) {
            mModulesConfig = new Config();
        } else {
            mModulesConfig = config;
        }

        if (mModulesConfig.isLoadDefaultReader()) {
            mPDFReader = new PDFReader(mContext, mPdfViewCtrl, mModulesConfig);
            registerModule(mPDFReader);
            mPDFReader.loadModule();
            parent = mPDFReader.getMainFrame().getContentView();
            registerToolHandlerChangedListener(mPDFReader.getToolHandlerChangedListener());

            mPanelManager = mPDFReader.getMainFrame().getPanelManager();
            if (mPDFReader.getMainFrame().getAttachedActivity() != null) {
                setAttachedActivity(mPDFReader.getMainFrame().getAttachedActivity());
            }
        }

        mParent = parent;

        if (mPanelManager == null) {
            mPanelManager = new PanelManager(context, mParent, null);
        }

        loadAllModules();
    }

    private void loadAllModules() {
        if (mModulesConfig.isLoadTextSelection()) {
            //text select module
            TextSelectModule tsModule = new TextSelectModule(mContext, mPdfViewCtrl, this);
            tsModule.loadModule();
        }

        if (mModulesConfig.isLoadAnnotations() || mModulesConfig.isLoadSignature()) {
            registerToolHandler(new BlankSelectToolHandler(mContext, mPdfViewCtrl, this));
        }

        if (mModulesConfig.isLoadAnnotations()) {
            //squiggly annotation module
            SquigglyModule sqgModule = new SquigglyModule(mContext, mPdfViewCtrl, this);
            sqgModule.loadModule();

            //strikeout annotation module
            StrikeoutModule stoModule = new StrikeoutModule(mContext, mPdfViewCtrl, this);
            stoModule.loadModule();

            //underline annotation module
            UnderlineModule unlModule = new UnderlineModule(mContext, mPdfViewCtrl, this);
            unlModule.loadModule();

            //highlight annotation module
            HighlightModule hltModule = new HighlightModule(mContext, mPdfViewCtrl, this);
            hltModule.loadModule();

            //note annotation module
            NoteModule noteModule = new NoteModule(mContext, mPdfViewCtrl, this);
            noteModule.loadModule();

            //link module
            LinkModule linkModule = new LinkModule(mContext, mPdfViewCtrl, this);
            linkModule.loadModule();

            //circle module
            CircleModule circleModule = new CircleModule(mContext, mPdfViewCtrl, this);
            circleModule.loadModule();

            //square module
            SquareModule squareModule = new SquareModule(mContext, mPdfViewCtrl, this);
            squareModule.loadModule();

            //freetext: typewriter
            TypewriterModule typewriterModule = new TypewriterModule(mContext, mPdfViewCtrl, this);
            typewriterModule.loadModule();

            //stamp module
            StampModule stampModule = new StampModule(mContext, mPdfViewCtrl, this);
            stampModule.loadModule();

            //Caret module
            CaretModule caretModule = new CaretModule(mContext, mPdfViewCtrl, this);
            caretModule.loadModule();

            //eraser module
            EraserModule eraserModule = new EraserModule(mContext, mPdfViewCtrl, this);
            eraserModule.loadModule();

            //ink(pencil) module
            InkModule inkModule = new InkModule(mContext, mPdfViewCtrl, this);
            inkModule.loadModule();

            //Line module
            LineModule lineModule = new LineModule(mContext, mPdfViewCtrl, this);
            lineModule.loadModule();

            //FileAttachment module
            FileAttachmentModule fileAttachmentModule = new FileAttachmentModule(mContext, mPdfViewCtrl, this);
            fileAttachmentModule.loadModule();

            //undo&redo module
            UndoModule undoModule = new UndoModule(mContext, mPdfViewCtrl, this);
            undoModule.loadModule();
        }
        if (mModulesConfig.isLoadPageNavigation()) {
            //page navigation module
            PageNavigationModule pageNavigationModule = new PageNavigationModule(mContext, mParent, mPdfViewCtrl, this);
            pageNavigationModule.loadModule();
        }

        if (mModulesConfig.isLoadForm()) {
            //form navigation module
            FormNavigationModule formNavigationModule = new FormNavigationModule(mContext, mParent, this);
            formNavigationModule.loadModule();
            //form annotation module
            FormFillerModule formFillerModule = new FormFillerModule(mContext, mParent, mPdfViewCtrl, this);
            formFillerModule.loadModule();
        }

        if (mModulesConfig.isLoadSignature()) {
            //signature module
            SignatureModule signatureModule = new SignatureModule(mContext, mParent, mPdfViewCtrl, this);
            signatureModule.loadModule();

            DigitalSignatureModule dsgModule = new DigitalSignatureModule(mContext, mParent, mPdfViewCtrl, this);
            dsgModule.loadModule();
        }

        if (mModulesConfig.isLoadSearch()) {
            SearchModule searchModule = new SearchModule(mContext, mParent, mPdfViewCtrl, this);
            searchModule.loadModule();
        }

        if (mModulesConfig.isLoadReadingBookmark()) {
            ReadingBookmarkModule readingBookmarkModule = new ReadingBookmarkModule(mContext, mParent, mPdfViewCtrl, this);
            readingBookmarkModule.loadModule();
        }

        if (mModulesConfig.isLoadOutline()) {
            OutlineModule outlineModule = new OutlineModule(mContext, mParent, mPdfViewCtrl ,this);
            outlineModule.loadModule();
        }

        if (mModulesConfig.isLoadAnnotations()) {
            //annot panel
            AnnotPanelModule annotPanelModule = new AnnotPanelModule(mContext, mPdfViewCtrl, this);
            annotPanelModule.loadModule();
        }

        if (mModulesConfig.isLoadAttachment()) {
            FileSpecPanelModule fileSpecPanelModule = new FileSpecPanelModule(mContext, mParent, mPdfViewCtrl, this);
            fileSpecPanelModule.loadModule();
        }

        if (mModulesConfig.isLoadThumbnail()) {
            ThumbnailModule thumbnailModule = new ThumbnailModule(mContext, mPdfViewCtrl, this);
            thumbnailModule.loadModule();
        }

        if (mModulesConfig.isLoadFileEncryption()) {
            //password module
            PasswordModule passwordModule = new PasswordModule(mContext, mPdfViewCtrl, this);
            passwordModule.loadModule();
        }

        ReflowModule reflowModule = new ReflowModule(mContext, mParent, mPdfViewCtrl, this);
        reflowModule.loadModule();

        DocInfoModule docInfoModule = new DocInfoModule(mContext, mParent, mPdfViewCtrl, this);
        docInfoModule.loadModule();

        BrightnessModule brightnessModule = new BrightnessModule(mContext, mPdfViewCtrl, this);
        brightnessModule.loadModule();

        ScreenLockModule screenLockModule = new ScreenLockModule(this);
        screenLockModule.loadModule();

        MoreMenuModule mMoreMenuModule = new MoreMenuModule(mContext, mParent, mPdfViewCtrl, this);
        mMoreMenuModule.loadModule();

        CropModule cropModule = new CropModule(mContext, mParent, mPdfViewCtrl, this);
        cropModule.loadModule();
    }

    public ViewGroup getRootView() {
        return mParent;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    public IPDFReader getPDFReader() {
        return mPDFReader;
    }

    /**
     * Get the PDF view control.
     *
     * @return A <CODE>PDFViewCtrl</CODE> object which indicates the current PDF view control.<br>
     */
    public PDFViewCtrl getPDFViewCtrl() {
        return mPdfViewCtrl;
    }

    /**
     * Register the {@link ToolHandler} changed listener.
     *
     * @param listener A <CODE>ToolHandlerChangedListener</CODE> object which specifies the {@link ToolHandler} changed listener.
     */
    public void registerToolHandlerChangedListener(ToolHandlerChangedListener listener) {
        mHandlerChangedListeners.add(listener);
    }

    /**
     * Unregister the {@link ToolHandler} changed listener.
     *
     * @param listener A <CODE>ToolHandlerChangedListener</CODE> object which specifies the {@link ToolHandler} changed listener.
     */
    public void unregisterToolHandlerChangedListener(ToolHandlerChangedListener listener) {
        mHandlerChangedListeners.remove(listener);
    }

    private void onToolHandlerChanged(ToolHandler lastTool, ToolHandler currentTool) {
        for (ToolHandlerChangedListener listener : mHandlerChangedListeners) {
            listener.onToolHandlerChanged(lastTool, currentTool);
        }
    }

    /**
     * register the {@link ConfigurationChangedListener}.
     *
     * @param listener
     */
    public void registerConfigurationChangedListener(ConfigurationChangedListener listener) {
        mConfigurationChangedListeners.add(listener);
    }

    /**
     * unregister the {@link ConfigurationChangedListener}.
     *
     * @param listener
     */
    public void unregisterConfigurationChangedListener(ConfigurationChangedListener listener) {
        mConfigurationChangedListeners.remove(listener);
    }

    /**
     * Dispatch configuration change to all modules.
     *
     * @param config
     */
    public void onConfigurationChanged(Configuration config) {
        for (ConfigurationChangedListener listener : mConfigurationChangedListeners) {
            listener.onConfigurationChanged(config);
        }
    }


    /**
     * Set the current tool handler.
     *
     * @param toolHandler A <CODE>ToolHandler</CODE> object which specifies the current tool handler.
     */
    public void setCurrentToolHandler(ToolHandler toolHandler) {
        if (toolHandler == null && mCurToolHandler == null) {
            return;
        }

        if (!DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot() ||
                (toolHandler != null && mCurToolHandler != null && mCurToolHandler.getType() == toolHandler.getType())) {
            return;
        }
        ToolHandler lastToolHandler = mCurToolHandler;
        if (lastToolHandler != null) {
            lastToolHandler.onDeactivate();
        }

        if (toolHandler != null) {
            if (DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot() != null) {
                DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
            }
        }

        mCurToolHandler = toolHandler;
        if (mCurToolHandler != null) {
            mCurToolHandler.onActivate();
        }
        onToolHandlerChanged(lastToolHandler, mCurToolHandler);
    }

    /**
     * Get the current tool handler.
     *
     * @return A <CODE>ToolHandler</CODE> object which specifies the current tool handler.
     */
    public ToolHandler getCurrentToolHandler() {
        return mCurToolHandler;
    }

    /**
     * Register the specified tool handler to current UI extensions manager.
     *
     * @param handler A <CODE>ToolHandler</CODE> object to be registered.
     */
    public void registerToolHandler(ToolHandler handler) {
        mToolHandlerList.put(handler.getType(), handler);
    }

    /**
     * Unregister the specified tool handler from current UI extensions manager.
     *
     * @param handler A <CODE>ToolHandler</CODE> object to be unregistered.
     */
    public void unregisterToolHandler(ToolHandler handler) {
        mToolHandlerList.remove(handler.getType());
    }

    /**
     * get the specified tool handler from current UI extensions manager.
     *
     * @param type The tool handler type, refer to function {@link ToolHandler#getType()}.
     * @return A <CODE>ToolHandler</CODE> object with specified type.
     */
    public ToolHandler getToolHandlerByType(String type) {
        return mToolHandlerList.get(type);
    }

    protected void registerAnnotHandler(AnnotHandler handler) {
        mAnnotHandlerList.put(handler.getType(), handler);
    }

    protected void unregisterAnnotHandler(AnnotHandler handler) {
        mAnnotHandlerList.remove(handler.getType());
    }

    protected AnnotHandler getCurrentAnnotHandler() {
        Annot curAnnot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        if (curAnnot == null) {
            return null;
        }

        return getAnnotHandlerByType(DocumentManager.getAnnotHandlerType(curAnnot));
    }

    protected AnnotHandler getAnnotHandlerByType(int type) {
        return mAnnotHandlerList.get(type);
    }

    /**
     * Register the specified module to current UI extensions manager.
     *
     * @param module A <CODE>Module</CODE> object to be registered.
     */
    public void registerModule(Module module) {
        mModules.add(module);
    }

    /**
     * Unregister the specified module from current UI extensions manager.
     *
     * @param module A <CODE>Module</CODE> object to be unregistered.
     */
    public void unregisterModule(Module module) {
        mModules.remove(module);
    }


    /**
     * Get the specified module from current UI extensions manager.
     *
     * @param name The specified module name, refer to {@link Module#getName()}.
     * @return A <CODE>Module</CODE> object with specified module name.
     */
    public Module getModuleByName(String name) {
        for (Module module : mModules) {
            String moduleName = module.getName();
            if (moduleName != null && moduleName.compareTo(name) == 0)
                return module;
        }
        return null;
    }

    /**
     * Enable link annotation action event.
     *
     * @param enable True means link annotation action event can be triggered, false for else.
     */
    public void enableLinks(boolean enable) {
        mEnableLinkAnnot = enable;
    }

    /**
     * Check whether link annotation action event can be triggered.
     *
     * @return True means link annotation action event can be triggered, false for else.
     */
    public boolean isLinksEnabled() {
        return mEnableLinkAnnot;
    }

    /**
     * Check whether link highlight can be display.
     *
     * @return True means link highlight can be display, false for else.
     */
    public boolean isLinkHighlightEnabled() {
        return mEnableLinkHighlight;
    }

    /**
     * Enable link highlight
     *
     * @param enable True means link highlight can be display, false for else.
     */
    public void enableLinkHighlight(boolean enable) {
        this.mEnableLinkHighlight = enable;
    }


    /**
     * get link highlight color
     *
     * @return link highlight color
     */
    public long getLinkHighlightColor() {
        return mLinkHighlightColor;
    }

    /**
     * set link highlight color
     *
     * @param color the link highlight color to be set
     */
    public void setLinkHighlightColor(long color) {
        this.mLinkHighlightColor = color;
    }

    /**
     * Check whether form highlight can be display.
     *
     * @return True means form highlight can be display, false for else.
     */
    public boolean isFormHighlightEnable() {
        return mEnableFormHighlight;
    }

    /**
     * get form highlight color
     *
     * @return form highlight color
     */
    public long getFormHighlightColor() {
        return mFormHighlightColor;
    }

    /**
     * Enable form highlight
     *
     * @param enable True means link highlight can be display, false for else.
     */
    public void enableFormHighlight(boolean enable) {
        this.mEnableFormHighlight = enable;
    }

    /**
     * set form highlight color
     *
     * @param color the form highlight color to be set
     */
    public void setFormHighlightColor(long color) {
        this.mFormHighlightColor = color;
    }

    /**
     * Set highlight color (including alpha) for text select tool handler.
     *
     * @param color The highlight color to be set.
     */
    public void setSelectionHighlightColor(int color) {
        mSelectHighlightColor = color;
    }

    /**
     * Get highlight color (including alpha) of text select tool handler.
     *
     * @return The highlight color.
     */
    public int getSelectionHighlightColor() {
        return mSelectHighlightColor;
    }

    /**
     * Get current selected text content from text select tool handler.
     *
     * @return The current selected text content.
     */
    public String getCurrentSelectedText() {
        ToolHandler selectionTool = getToolHandlerByType(ToolHandler.TH_TYPE_TEXTSELECT);
        if (selectionTool != null) {
            return ((TextSelectToolHandler) selectionTool).getCurrentSelectedText();
        }

        return null;
    }

    public void registerMenuEventListener(MenuEventListener listener) {
        mMenuEventListeners.add(listener);
    }

    public void unregisterMenuEventListener(MenuEventListener listener) {
        mMenuEventListeners.remove(listener);
    }

    /**
     * Trigger dismiss menu event.
     */
    public void triggerDismissMenuEvent() {
        for (MenuEventListener listener : mMenuEventListeners) {
            listener.onTriggerDismissMenu();
        }
    }

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent) {
        if (mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_REFLOW)
            return false;
        if (motionEvent.getPointerCount() > 1) {
            return false;
        }

        if (mCurToolHandler != null) {
            if (mCurToolHandler.onTouchEvent(pageIndex, motionEvent)) {
                return true;
            }
            return false;
        } else {
            //annot handler
            if (DocumentManager.getInstance(mPdfViewCtrl).onTouchEvent(pageIndex, motionEvent)) {
                return true;
            }

            //blank selection tool
            ToolHandler blankSelectionTool = getToolHandlerByType(ToolHandler.TH_TYPE_BLANKSELECT);
            if (blankSelectionTool != null && blankSelectionTool.onTouchEvent(pageIndex, motionEvent)) {
                return true;
            }

            //text selection tool
            ToolHandler textSelectionTool = getToolHandlerByType(ToolHandler.TH_TYPE_TEXTSELECT);
            if (textSelectionTool != null && textSelectionTool.onTouchEvent(pageIndex, motionEvent)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean shouldViewCtrlDraw(Annot annot) {
        return DocumentManager.getInstance(mPdfViewCtrl).shouldViewCtrlDraw(annot);
    }

    @Override
    public Annot getFocusAnnot() {
        return DocumentManager.getInstance(mPdfViewCtrl).getFocusAnnot();
    }

    @SuppressLint("WrongCall")
    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        for (ToolHandler handler : mToolHandlerList.values()) {
            handler.onDraw(pageIndex, canvas);
        }

        for (int i = 0; i < mAnnotHandlerList.size(); i++) {
            int type = mAnnotHandlerList.keyAt(i);
            AnnotHandler handler = mAnnotHandlerList.get(type);
            if (handler != null)
                handler.onDraw(pageIndex, canvas);
        }
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
        if (mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_REFLOW)
            return false;
        if (motionEvent.getPointerCount() > 1) {
            return false;
        }
        PointF displayViewPt = new PointF(motionEvent.getX(), motionEvent.getY());
        int pageIndex = mPdfViewCtrl.getPageIndex(displayViewPt);


        if (mCurToolHandler != null) {
            if (mCurToolHandler.onSingleTapConfirmed(pageIndex, motionEvent)) {
                return true;
            }
            return false;
        } else {
            //annot handler
            if (DocumentManager.getInstance(mPdfViewCtrl).onSingleTapConfirmed(pageIndex, motionEvent)) {
                return true;
            }

            // blank selection tool
            ToolHandler blankSelectionTool = getToolHandlerByType(ToolHandler.TH_TYPE_BLANKSELECT);
            if (blankSelectionTool != null && blankSelectionTool.onSingleTapConfirmed(pageIndex, motionEvent)) {
                return true;
            }

            //text selection tool
            ToolHandler textSelectionTool = getToolHandlerByType(ToolHandler.TH_TYPE_TEXTSELECT);
            if (textSelectionTool != null && textSelectionTool.onSingleTapConfirmed(pageIndex, motionEvent)) {
                return true;
            }

            if (DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot() != null) {
                DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {
        if (mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_REFLOW)
            return;
        if (motionEvent.getPointerCount() > 1) {
            return;
        }
        PointF displayViewPt = new PointF(motionEvent.getX(), motionEvent.getY());
        int pageIndex = mPdfViewCtrl.getPageIndex(displayViewPt);

        if (mCurToolHandler != null) {
            if (mCurToolHandler.onLongPress(pageIndex, motionEvent)) {
                return;
            }
        } else {
            //annot handler
            if (DocumentManager.getInstance(mPdfViewCtrl).onLongPress(pageIndex, motionEvent)) {
                return;
            }

            // blank selection tool
            ToolHandler blankSelectionTool = getToolHandlerByType(ToolHandler.TH_TYPE_BLANKSELECT);
            if (blankSelectionTool != null && blankSelectionTool.onLongPress(pageIndex, motionEvent)) {
                return;
            }

            //text selection tool
            ToolHandler textSelectionTool = getToolHandlerByType(ToolHandler.TH_TYPE_TEXTSELECT);
            if (textSelectionTool != null && textSelectionTool.onLongPress(pageIndex, motionEvent)) {
                return;
            }
        }
        return;
    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

    @Override
    public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
        return false;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
        return false;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
    }

    PDFViewCtrl.IDoubleTapEventListener mDoubleTapEventListener = new PDFViewCtrl.IDoubleTapEventListener() {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
            if (DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot() != null) {
                DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                return true;
            }
            return false;
        }

        @Override
        public boolean onDoubleTap(MotionEvent motionEvent) {
            return false;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent motionEvent) {
            return false;
        }
    };

    PDFViewCtrl.IDocEventListener mDocEventListener = new PDFViewCtrl.IDocEventListener() {
        @Override
        public void onDocWillOpen() {

        }

        @Override
        public void onDocOpened(PDFDoc document, int errCode) {
            if (errCode != PDFError.NO_ERROR.getCode() || document == null) {
                return;
            }

            DocumentManager.getInstance(mPdfViewCtrl).initDocProperties(document);
        }

        @Override
        public void onDocWillClose(PDFDoc document) {

        }

        @Override
        public void onDocClosed(PDFDoc document, int errCode) {

        }

        @Override
        public void onDocWillSave(PDFDoc document) {

        }

        @Override
        public void onDocSaved(PDFDoc document, int errCode) {

        }
    };


    PDFViewCtrl.IRecoveryEventListener mRecoveryEventListener = new PDFViewCtrl.IRecoveryEventListener() {

        @Override
        public void onWillRecover() {
            DocumentManager.getInstance(mPdfViewCtrl).mCurAnnot = null;
            DocumentManager.getInstance(mPdfViewCtrl).clearUndoRedo();
            BlankSelectToolHandler toolHandler = (BlankSelectToolHandler) getToolHandlerByType(ToolHandler.TH_TYPE_BLANKSELECT);
            if (toolHandler != null) {
                toolHandler.dismissMenu();
            }
        }

        @Override
        public void onRecovered() {
            DocumentManager.getInstance(mPdfViewCtrl).initDocProperties(mPdfViewCtrl.getDoc());
        }
    };

    /**
     * Set the attached activity.
     * <p>
     * If you want add a Note, FreeText, FileAttachment annotation; you must set the attached activity.
     * <p>
     * If you want to use the function of adding reply or comment to the annotation or about thumbnail,
     * you must set the attached activity and it must be a FragmentActivity.
     *
     * @param activity The attached activity.
     */
    public void setAttachedActivity(Activity activity) {
        mAttachActivity = activity;
    }

    /**
     * Get the attached activity.
     *
     * @return The attached activity.
     */
    public Activity getAttachedActivity() {
        return mAttachActivity;
    }

    public void destroy() {
        BlankSelectToolHandler selectToolHandler = (BlankSelectToolHandler) getToolHandlerByType(ToolHandler.TH_TYPE_BLANKSELECT);
        if (selectToolHandler != null) {
            selectToolHandler.unload();
            unregisterToolHandler(selectToolHandler);
        }

        for (Module module : mModules) {
            if (module instanceof LocalModule) continue;
            module.unloadModule();
        }

        mModules.clear();
        mModules = null;
        mPdfViewCtrl.unregisterDocEventListener(mDocEventListener);
        mPdfViewCtrl.unregisterRecoveryEventListener(mRecoveryEventListener);
        mPdfViewCtrl.unregisterDoubleTapEventListener(mDoubleTapEventListener);
        unregisterMenuEventListener(mMenuEventListener);
        DocumentManager.getInstance(mPdfViewCtrl).destroy();
        DocumentManager.release();

        mMenuEventListeners.clear();
        mCurToolHandler = null;
        mModulesConfig = null;
        mPanelManager = null;
        mPDFReader = null;
        mPdfViewCtrl = null;
        mAttachActivity = null;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        BlankSelectToolHandler selectToolHandler = (BlankSelectToolHandler) getToolHandlerByType(ToolHandler.TH_TYPE_BLANKSELECT);
        if (selectToolHandler != null) {
            return selectToolHandler.onKeyDown(keyCode, event);
        }

        TextSelectModule textSelectModule = (TextSelectModule) getModuleByName(Module.MODULE_NAME_SELECTION);
        if (textSelectModule != null) {
            return textSelectModule.onKeyDown(keyCode, event);
        }
        return false;
    }

    public IPanelManager getPanelManager() {
        return mPanelManager;
    }


    UIExtensionsManager.MenuEventListener mMenuEventListener = new UIExtensionsManager.MenuEventListener() {
        @Override
        public void onTriggerDismissMenu() {
            if (DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot() != null) {
                DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
            }
        }
    };

    public Config getModulesConfig() {
        return mModulesConfig;
    }

    public static final class Config {
        private static final String KEY_DEFAULTREADER = "defaultReader";
        private static final String KEY_MODULES = "modules";

        private static final String KEY_MODULE_READINGBOOKMARK = "readingbookmark";
        private static final String KEY_MODULE_OUTLINE = "outline";
        private static final String KEY_MODULE_ANNOTATIONS = "annotations";
        private static final String KEY_MODULE_THUMBNAIL = "thumbnail";
        private static final String KEY_MODULE_ATTACHMENT = "attachment";
        private static final String KEY_MODULE_SIGNATURE = "signature";
        private static final String KEY_MODULE_SEARCH = "search";
        private static final String KEY_MODULE_SELECTION = "selection";
        private static final String KEY_MODULE_PAGENAVIGATION = "pageNavigation";
        private static final String KEY_MODULE_ENCRYPTION = "encryption";
        private static final String KEY_MODULE_FORM = "form";

        private boolean isLoadDefaultReader = false;
        private boolean isLoadReadingBookmark = true;
        private boolean isLoadOutline = true;
        private boolean isLoadAnnotations = true;
        private boolean isLoadThumbnail = true;
        private boolean isLoadAttachment = true;
        private boolean isLoadSignature = true;
        private boolean isLoadSearch = true;
        private boolean isLoadTextSelection = true;
        private boolean isLoadPageNavigation = true;
        private boolean isLoadFileEncryption = true;
        private boolean isLoadForm = true;

        protected Config() {
        }

        public Config(@NonNull InputStream stream) {
            read(stream);
        }

        private boolean read(InputStream stream) {
            byte[] buffer = new byte[1 << 13];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int n = 0;
            try {
                while (-1 != (n = stream.read(buffer))) {
                    baos.write(buffer, 0, n);
                }

                String config = baos.toString();
                if (config != null || config.trim().length() > 1) {
                    parseConfig(config);
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } finally {
                try {
                    baos.flush();
                    baos.close();
                } catch (IOException e) {
                }
            }

            return true;
        }

        private void parseConfig(@NonNull String config) {
            try {
                JSONObject jsonObject = new JSONObject(config);
                isLoadDefaultReader = getBooleanFromConfigModules(jsonObject, KEY_DEFAULTREADER, false);

                if (jsonObject.has(KEY_MODULES)) {
                    JSONObject modules = jsonObject.getJSONObject(KEY_MODULES);

                    if (null != modules) {
                        isLoadReadingBookmark = getBooleanFromConfigModules(modules, KEY_MODULE_READINGBOOKMARK, true);
                        isLoadOutline = getBooleanFromConfigModules(modules, KEY_MODULE_OUTLINE, true);
                        isLoadAnnotations = getBooleanFromConfigModules(modules, KEY_MODULE_ANNOTATIONS, true);
                        isLoadThumbnail = getBooleanFromConfigModules(modules, KEY_MODULE_THUMBNAIL, true);
                        isLoadAttachment = getBooleanFromConfigModules(modules, KEY_MODULE_ATTACHMENT, true);
                        isLoadSignature = getBooleanFromConfigModules(modules, KEY_MODULE_SIGNATURE, true);
                        isLoadSearch = getBooleanFromConfigModules(modules, KEY_MODULE_SEARCH, true);
                        isLoadTextSelection = getBooleanFromConfigModules(modules, KEY_MODULE_SELECTION, true);
                        isLoadPageNavigation = getBooleanFromConfigModules(modules, KEY_MODULE_PAGENAVIGATION, true);
                        isLoadFileEncryption = getBooleanFromConfigModules(modules, KEY_MODULE_ENCRYPTION, true);
                        isLoadForm = getBooleanFromConfigModules(modules, KEY_MODULE_FORM, true);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public boolean isLoadDefaultReader() {
            return isLoadDefaultReader;
        }

        public boolean isLoadReadingBookmark() {
            return isLoadReadingBookmark;
        }

        public boolean isLoadOutline() {
            return isLoadOutline;
        }

        public boolean isLoadAnnotations() {
            return isLoadAnnotations;
        }

        public boolean isLoadThumbnail() {
            return isLoadThumbnail;
        }

        public boolean isLoadAttachment() {
            return isLoadAttachment;
        }

        public boolean isLoadSignature() {
            return isLoadSignature;
        }

        public boolean isLoadSearch() {
            return isLoadSearch;
        }

        public boolean isLoadTextSelection() {
            return isLoadTextSelection;
        }

        public boolean isLoadPageNavigation() {
            return isLoadPageNavigation;
        }

        public boolean isLoadFileEncryption() {
            return isLoadFileEncryption;
        }

        public boolean isLoadForm() {
            return isLoadForm;
        }

        private boolean getBooleanFromConfigModules(JSONObject modules, String name, boolean defaultValue) {
            try {
                if (modules.has(name) && modules.get(name) instanceof Boolean) {
                    boolean isLoadModule = modules.getBoolean(name);
                    return isLoadModule;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return defaultValue;
        }
    }
}
