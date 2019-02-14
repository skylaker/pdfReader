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
package com.foxit.uiextensions.modules.panel.filespec;

import android.app.Activity;
import android.content.Context;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.FileSpec;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.FileAttachment;
import com.foxit.sdk.pdf.objects.PDFDictionary;
import com.foxit.sdk.pdf.objects.PDFNameTree;
import com.foxit.sdk.pdf.objects.PDFObject;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.fileattachment.FileAttachmentUtil;
import com.foxit.uiextensions.browser.adapter.FileAttachmentAdapter;
import com.foxit.uiextensions.modules.panel.bean.FileBean;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppFileUtil;
import com.foxit.uiextensions.utils.AppIntentUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

import java.io.File;
import java.util.ArrayList;

/**
 * <br><time>25/4/17</time>
 *
 * @see
 */
public class FileAttachmentPresenter {

    private FileAttachmentViewer viewer;

    private PDFNameTree pdfNameTree;

    private PDFDoc pdfDoc;

    private ArrayList<FileBean> list;

    private ArrayList<Annot> annotList;

    private Context context;
    private PDFViewCtrl mAttachPdfViewCtrl;

    public FileAttachmentPresenter(Context context, FileAttachmentViewer viewer) {
        this.context = context;
        this.viewer = viewer;
        list = new ArrayList<>();
        annotList = new ArrayList<>();
    }

    public void setPdfDoc(PDFDoc pdfDoc) {
        this.pdfDoc = pdfDoc;
    }

    public ArrayList<Annot> getAnnotList() {
        return annotList;
    }

    public void init(boolean isLoadAnnotation) {
        list.clear();
        annotList.clear();
        try {
            this.pdfNameTree = PDFNameTree.create(pdfDoc, PDFNameTree.e_nameTreeEmbeddedFiles);
        } catch (PDFException e) {
            e.printStackTrace();
        }
        try {
            int nOrgCount = pdfNameTree.getCount();
            if (nOrgCount > 0) {
                FileBean fb = new FileBean();
                fb.setFlag(FileAttachmentAdapter.FLAG_TAG);
                fb.setTag("Attachment Tab");
                list.add(fb);
            }
            for (int o = 0; o < nOrgCount; o++) {
                String name = pdfNameTree.getName(o);
                PDFObject object = pdfNameTree.getObj(name);
                FileBean item = new FileBean();
                FileSpec fs = FileSpec.createFromPDFObj(pdfDoc, object);
                item.setName(name);
                item.setTitle(fs.getFileName());
                item.setSize(AppDmUtil.getLocalDateString(fs.getModifiedDateTime()) + " " + AppFileUtil.formatFileSize(fs.getFileSize()));
                item.setFlag(FileAttachmentAdapter.FLAG_NORMAL);
                item.setDesc(fs.getDescription());
                list.add(item);
            }

        } catch (PDFException e) {
            e.printStackTrace();
        }

        //load annot
        if (isLoadAnnotation) {
            try {
                int pagecount = pdfDoc.getPageCount();
                for (int i = 0; i < pagecount; i++) {
                    PDFPage pdfPage = pdfDoc.getPage(i);
                    int annotcount = pdfPage.getAnnotCount();
                    int count = 0;
                    for (int j = 0; j < annotcount; j++) {
                        Annot annot = pdfPage.getAnnot(j);
                        if (annot.getType() == Annot.e_annotFileAttachment) {
                            count += 1;
                            FileSpec fileSpec = ((FileAttachment) annot).getFileSpec();
                            FileBean item = new FileBean();
                            item.setTitle(fileSpec.getFileName());
                            item.setName(fileSpec.getFileName());
                            item.setSize(AppDmUtil.getLocalDateString(fileSpec.getModifiedDateTime()) + " " + AppFileUtil.formatFileSize(fileSpec.getFileSize()));
                            item.setFlag(FileAttachmentAdapter.FLAG_ANNOT);
                            item.setDesc(fileSpec.getDescription());
                            item.setUuid(annot.getUniqueID());
                            list.add(item);
                            annotList.add(annot);
                        }
                    }
                    if (count > 0) {
                        FileBean fb = new FileBean();
                        fb.setFlag(FileAttachmentAdapter.FLAG_TAG);
                        int pageIndex = i+1;
                        fb.setTag("page " + pageIndex);
                        list.add(list.size() - count, fb);
                    }
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }

        viewer.success(list);
    }

    private String rename(String name) throws PDFException {
        if (!pdfNameTree.hasName(name))
            return name;
        String oldName = name.substring(0, name.lastIndexOf('.'));
        String copyName = oldName + "-Copy";
        name = name.replace(oldName, copyName);
        return rename(name);
    }

    public void add(String name, String path) {
        FileSpec pNewFile;
        try {
            name = rename(name);
            int index = 0;
            boolean insert = false;
            for (FileBean b : list) {
                index += 1;
                if (b.getFlag() == FileAttachmentAdapter.FLAG_NORMAL) {
                    insert = true;
                    break;
                }
            }

            pNewFile = FileSpec.create(pdfDoc);
            pNewFile.setFileName(name);
            pNewFile.embed(path);
            pNewFile.setCreationDateTime(AppDmUtil.currentDateToDocumentDate());
            pNewFile.setModifiedDateTime(AppDmUtil.javaDateToDocumentDate(new File(path).lastModified()));
            PDFDictionary dict = pNewFile.getDict();
            pdfNameTree.add(pNewFile.getFileName(), dict);

            FileBean item = new FileBean();
            item.setTitle(pNewFile.getFileName());
            item.setName(pNewFile.getFileName());
            item.setSize(AppDmUtil.getLocalDateString(pNewFile.getModifiedDateTime()) + " " + AppFileUtil.formatFileSize(pNewFile.getFileSize()));
            item.setFlag(FileAttachmentAdapter.FLAG_NORMAL);
            item.setDesc(pNewFile.getDescription());


            if (!insert) {
                FileBean fb = new FileBean();
                fb.setFlag(FileAttachmentAdapter.FLAG_TAG);
                fb.setTag("Attachment Tab");
                list.add(fb);
                list.add(item);
            } else {
                list.add(index+pdfNameTree.getCount()-2,item);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

        viewer.success(list);

    }

    public void add(Annot annot) {
        annotList.add(annot);
        try {
            int pageIndex = annot.getPage().getIndex()+1;
            int index = 0;
            boolean insert = false;
            for (FileBean b : list) {
                index += 1;
                if (b.getFlag() == FileAttachmentAdapter.FLAG_TAG && b.getTag().endsWith("" + pageIndex)) {
                    insert = true;
                    break;
                }
            }

            FileSpec pNewFile = ((FileAttachment) annot).getFileSpec();
            FileBean item = new FileBean();
            item.setTitle(pNewFile.getFileName());
            item.setName(pNewFile.getFileName());
            item.setSize(AppDmUtil.getLocalDateString(pNewFile.getModifiedDateTime()) + " " + AppFileUtil.formatFileSize(pNewFile.getFileSize()));
            item.setFlag(FileAttachmentAdapter.FLAG_ANNOT);
            item.setDesc(pNewFile.getDescription());
            item.setUuid(annot.getUniqueID());

            if (!insert) {
                FileBean fb = new FileBean();
                fb.setFlag(FileAttachmentAdapter.FLAG_TAG);
                fb.setTag("page " + pageIndex);
                list.add(fb);
                list.add(item);
            } else {
                list.add(index, item);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

        viewer.success(list);
    }

    public void update(int index, String content) {
        //update file panel file
        if (list.get(index).getFlag() == FileAttachmentAdapter.FLAG_NORMAL) {
            try {
                PDFObject obj = pdfNameTree.getObj(list.get(index).getTitle());
                FileSpec pNewFile = FileSpec.createFromPDFObj(pdfDoc, obj);
                pNewFile.setDescription(content);

                pdfNameTree.setObj(list.get(index).getTitle(), pNewFile.getDict());
                list.get(index).setDesc(content);
            } catch (PDFException e) {
                e.printStackTrace();
            }
        } else if (list.get(index).getFlag() == FileAttachmentAdapter.FLAG_ANNOT) {
            //update annot file
            try {
                FileBean i = list.get(index);
                String uuid = i.getUuid();
                for (Annot a : annotList) {
                    if (uuid.equals(a.getUniqueID())) {
                        FileSpec fs = ((FileAttachment) a).getFileSpec();
                        fs.setDescription(content);
                        list.get(index).setDesc(content);
                    }
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }
        viewer.success(list);

    }

    private void clearTag(String content){
        for (FileBean item : list){
            if (item.getFlag()==FileAttachmentAdapter.FLAG_TAG && content.equals(item.getTag())){
                list.remove(item);
                break;
            }
        }
    }

    public void delete(int count, int start, int end) {
        if (count == 0) {
            viewer.fail(FileAttachmentViewer.DELETE, null);
        }
        boolean clear = false;
        for (int i = start; i <= end; i++) {
            FileBean bean = list.get(i);
            if (bean.getFlag() == FileAttachmentAdapter.FLAG_TAG) {
                //list.remove(i);
                continue;
            }
            try {
                pdfNameTree.removeObj(list.get(i).getTitle());
                list.remove(i);
                if (pdfNameTree.getCount() == 0){
                    clear = true;
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }
        if (clear){
            clearTag("Attachment Tab");
        }
        viewer.success(list);
    }

    public void delete(PDFViewCtrl pdfViewCtrl, Annot annot) {
        if(!annotList.contains(annot)){
            return;
        }
        boolean clear = true;
        int index = 0;
        try {
            index = annot.getPage().getIndex()+1;
            for (FileBean b : list) {
                if (!AppUtil.isBlank(b.getUuid()) && b.getUuid().equals(annot.getUniqueID())) {
                    list.remove(b);
                    break;
                }
            }
            annotList.remove(annot);
            DocumentManager.getInstance(pdfViewCtrl).removeAnnot(annot, true, null);

            for (Annot item: annotList){
                if(item.getPage().getIndex() == annot.getPage().getIndex()) {
                    clear = false;
                    break;
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

        if (clear){
            clearTag("page "+index);
        }

        viewer.success(list);
    }

    public void deleteByOutside(PDFViewCtrl pdfViewCtrl, Annot annot) {
        if(!annotList.contains(annot)){
            return;
        }
        boolean clear = true;
        int index = 0;
        try {
            index = annot.getPage().getIndex()+1;
            for (FileBean b : list) {
                if (!AppUtil.isBlank(b.getUuid()) && b.getUuid().equals(annot.getUniqueID())) {
                    list.remove(b);
                    break;
                }
            }
            annotList.remove(annot);
            for (Annot item: annotList){
                if(item.getPage().getIndex() == annot.getPage().getIndex()) {
                    clear = false;
                    break;
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

        if (clear){
            clearTag("page "+index);
        }

        viewer.success(list);
    }


    public void save(final PDFViewCtrl pdfViewCtrl, final int index, final String path) {
        if (list.get(index).getFlag() == FileAttachmentAdapter.FLAG_NORMAL) {
            try {
                FileSpec fileSpec = FileSpec.createFromPDFObj(pdfDoc, pdfNameTree.getObj(list.get(index).getName()));
                FileAttachmentUtil.saveAttachment(pdfViewCtrl, path, fileSpec, new Event.Callback() {
                    @Override
                    public void result(Event event, boolean success) {
                    }

                });

            } catch (PDFException e) {
                e.printStackTrace();
            }
        } else if (list.get(index).getFlag() == FileAttachmentAdapter.FLAG_ANNOT) {
            try {
                FileBean i = list.get(index);
                String uuid = i.getUuid();
                for (Annot a : annotList) {
                    if (uuid.equals(a.getUniqueID())) {
                        FileSpec fileSpec = ((FileAttachment) a).getFileSpec();
                        FileAttachmentUtil.saveAttachment(pdfViewCtrl, path, fileSpec, new Event.Callback() {
                            @Override
                            public void result(Event event, boolean success) {
                            }

                        });

                    }
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean endsWith(String str, String suffix, boolean ignoreCase) {
        if (str != null && suffix != null) {
            if (suffix.length() > str.length()) {
                return false;
            } else {
                int strOffset = str.length() - suffix.length();
                return str.regionMatches(ignoreCase, strOffset, suffix, 0, suffix.length());
            }
        } else {
            return str == null && suffix == null;
        }
    }

    public void open(final PDFViewCtrl pdfViewCtrl, final int index, String p) {
//        path = path  + UUID.randomUUID().toString().split("-")[0] + ".pdf";
        final String path = p + list.get(index).getTitle();
        if (list.get(index).getFlag() == FileAttachmentAdapter.FLAG_NORMAL) {
            try {
                final FileSpec fileSpec = FileSpec.createFromPDFObj(pdfDoc, pdfNameTree.getObj(list.get(index).getName()));
                FileAttachmentUtil.saveAttachment(pdfViewCtrl, path, fileSpec, new Event.Callback() {
                    @Override
                    public void result(Event event, boolean success) {
                        if (success) {
                            String ExpName = path.substring(path.lastIndexOf('.') + 1).toLowerCase();
                            if (ExpName.equals("pdf")) {
                                viewer.open(path, list.get(index).getTitle());
                            } else {
                                if (pdfViewCtrl.getUIExtensionsManager() == null) return;
                                Context context = ((UIExtensionsManager) pdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
                                if (context == null) return;
                                AppIntentUtil.openFile((Activity) context, path);
                            }
                        }
                    }

                });

            } catch (PDFException e) {
                e.printStackTrace();
            }
        } else if (list.get(index).getFlag() == FileAttachmentAdapter.FLAG_ANNOT) {
            try {
                FileBean i = list.get(index);
                String uuid = i.getUuid();
                for (Annot a : annotList) {
                    if (uuid.equals(a.getUniqueID())) {
                        FileSpec fileSpec = ((FileAttachment) a).getFileSpec();
                        FileAttachmentUtil.saveAttachment(pdfViewCtrl, path, fileSpec, new Event.Callback() {
                            @Override
                            public void result(Event event, boolean success) {
                                if (success) {
                                    String ExpName = path.substring(path.lastIndexOf('.') + 1).toLowerCase();
                                    if (ExpName.equals("pdf")) {
                                        viewer.open(path, list.get(index).getTitle());
                                    } else {
                                        if (pdfViewCtrl.getUIExtensionsManager() == null) return;
                                        Context context = ((UIExtensionsManager) pdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
                                        if (context == null) return;
                                        AppIntentUtil.openFile((Activity) context, path);
                                    }
                                }
                            }

                        });

                    }
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * viewer
     */
    public interface FileAttachmentViewer {
        int LOAD = 1;
        int DELETE = 2;
        int RENAME = 3;
        int CLEAR = 4;

        void success(ArrayList<FileBean> list);

        void fail(int rct, Object o);

        void open(String path, String name);
    }
}
