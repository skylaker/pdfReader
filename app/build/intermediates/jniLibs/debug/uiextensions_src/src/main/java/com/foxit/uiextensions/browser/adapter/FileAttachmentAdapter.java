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
package com.foxit.uiextensions.browser.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.text.Selection;
import android.text.Spannable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.browser.adapter.viewholder.PageFlagViewHolder;
import com.foxit.uiextensions.browser.adapter.viewholder.SuperViewHolder;
import com.foxit.uiextensions.controls.dialog.UITextEditDialog;
import com.foxit.uiextensions.controls.dialog.fileselect.UISaveAsDialog;
import com.foxit.uiextensions.modules.panel.bean.BaseBean;
import com.foxit.uiextensions.modules.panel.bean.FileBean;
import com.foxit.uiextensions.modules.panel.filespec.FileAttachmentPresenter;
import com.foxit.uiextensions.modules.panel.filespec.FileSpecModuleCallback;
import com.foxit.uiextensions.utils.AppKeyboardUtil;
import com.foxit.uiextensions.utils.AppUtil;

import java.util.ArrayList;
import java.util.List;

import static com.foxit.uiextensions.controls.dialog.fileselect.UISaveAsDialog.ISaveAsOnOKClickCallBack;

/**
 * <br><time>22/4/17</time>
 *
 * @see
 */
public class FileAttachmentAdapter extends SuperAdapter implements FileAttachmentPresenter.FileAttachmentViewer {

    private final static String TAG = FileAttachmentAdapter.class.getSimpleName();

    public final static int FLAG_NORMAL = 0;
    public final static int FLAG_TAG = 1;
    public final static int FLAG_ANNOT = 2;

    private List<FileBean> list;
    private PDFDoc pdfDoc;

    private FileAttachmentPresenter presenter;
    private FileSpecModuleCallback callback;

    private PDFViewCtrl mPdfViewCtrl;

    private int index = -1;

    public FileAttachmentAdapter(Context context, List list, PDFViewCtrl pdfViewCtrl,FileSpecModuleCallback callback) {
        super(context);
        this.list = list;
        this.callback = callback;
        this.mPdfViewCtrl = pdfViewCtrl;
        presenter = new FileAttachmentPresenter(context,this);
    }

    public void setPdfDoc(PDFDoc pdfDoc) {
        this.pdfDoc = pdfDoc;
        presenter.setPdfDoc(pdfDoc);
    }

    public void reset(){
        index =-1;
    }

    /**
     * init data, load all pdf-name-tree item
     */
    public void init(boolean isLoadAnnotation) {
        presenter.init(isLoadAnnotation);
    }

    /**
     *  add a file spec to pdfdoc nametree
     * @param name file name
     * @param file file path
     */
    public void add(String name, String file){
        presenter.add(name,file);
    }

    public void add(Annot annot){
        presenter.add(annot);
    }

    private void delete(int count,int start, int end){
        presenter.delete(count,start,end);
    }

    //delete annot
    public void delete(Annot annot){
        if(!presenter.getAnnotList().contains(annot)){
            return;
        }
        presenter.delete(mPdfViewCtrl,annot);
    }

    public void deleteByOutside(Annot annot){
        if(!presenter.getAnnotList().contains(annot)){
            return;
        }
        presenter.deleteByOutside(mPdfViewCtrl,annot);
    }

    public void delete(int index){
        try {
            for (Annot a : presenter.getAnnotList()) {
                if (a.getUniqueID().equals(list.get(index).getUuid())){
                    delete(a);
                    break;
                }
            }
        }catch (PDFException e){
            e.printStackTrace();
        }
    }

    private void setDesc(int index,String content){
        presenter.update(index,content);
    }

    private void save(int index,String path){
        presenter.save(mPdfViewCtrl,index, path);
    }

    private void open(int index,String path){
        presenter.open(mPdfViewCtrl,index, path);
    }

    public List<FileBean> getList() {
        return list;
    }

    public void setList(List<FileBean> list) {
        this.list = list;
    }

    @Override
    public void notifyUpdateData() {
        notifyDataSetChanged();
    }

    @Override
    public SuperViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        SuperViewHolder viewHolder;
        switch (viewType) {
            case FLAG_NORMAL:
                viewHolder = new ItemViewHolder(inflateLayout(getContext(), parent, R.layout.panel_item_fileattachment));
                break;
            case FLAG_TAG:
                viewHolder = new PageFlagViewHolder(inflateLayout(getContext(), parent, R.layout.panel_item_fileattachment_flag));
                break;
            default:
                viewHolder = new ItemViewHolder(inflateLayout(getContext(), parent, R.layout.panel_item_fileattachment));
                break;
        }

        return viewHolder;
    }

    private View inflateLayout(Context context, ViewGroup parent, int layoutId) {
        return LayoutInflater.from(context).inflate(layoutId, parent, false);
    }


    @Override
    public int getItemCount() {
        return list.size();
    }

    @Override
    public FileBean getDataItem(int position) {
        return list.get(position);
    }

    @Override
    public int getItemViewType(int position) {
        return list.get(position).getFlag();
    }


    @Override
    public void success(ArrayList<FileBean> list) {
        this.list = list;
        this.index = -1;
        notifyDataSetChanged();
        if (this.list.size()>=1){
            callback.success();
        }
        if (this.list.size() == 0){
            callback.fail();
        }
    }

    @Override
    public void fail(int rct, Object o) {

    }

    @Override
    public void open(String path,String filename) {
        callback.open(path,filename);
    }


    class ItemViewHolder extends SuperViewHolder {

        private ImageView icon;
        private ImageView more;
        private TextView title;
        private TextView date_size;
        private TextView desc;
        private View view;
        private View container;

        private TextView more_save;
        private TextView more_desc;
        private TextView more_delete;



        public ItemViewHolder(View viewHolder) {
            super(viewHolder);
            container = viewHolder.findViewById(R.id.panel_attachment_container);
            icon = (ImageView) viewHolder.findViewById(R.id.panel_item_fileattachment_icon);
            more = (ImageView) viewHolder.findViewById(R.id.panel_item_fileattachment_more);
            title = (TextView) viewHolder.findViewById(R.id.panel_item_fileattachment_title);
            date_size = (TextView) viewHolder.findViewById(R.id.panel_item_fileattachment_date_size);
            desc = (TextView) viewHolder.findViewById(R.id.panel_item_fileattachment_desc);
            more.setOnClickListener(this);
            view = viewHolder.findViewById(R.id.more_view);
            more_save = (TextView) viewHolder.findViewById(R.id.panel_more_tv_save);
            more_desc = (TextView) viewHolder.findViewById(R.id.panel_more_tv_desc);
            more_delete = (TextView) viewHolder.findViewById(R.id.panel_more_tv_delete);

            more_save.setOnClickListener(this);
            more_delete.setOnClickListener(this);
            more_desc.setOnClickListener(this);
            container.setOnClickListener(this);

        }

        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.panel_item_fileattachment_more) {
                int temp = index;
                index = getAdapterPosition();
                notifyItemChanged(temp);
                notifyItemChanged(index);
            }

            if (v.getId() == R.id.panel_more_tv_desc){
                final UITextEditDialog textDialog = new UITextEditDialog(((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity());
                textDialog.getPromptTextView().setVisibility(View.GONE);
                textDialog.getInputEditText().setText(desc.getText());
                CharSequence text = textDialog.getInputEditText().getText();
                if (text instanceof Spannable) {
                    Spannable spanText = (Spannable)text;
                    Selection.setSelection(spanText, text.length());
                }
                textDialog.setTitle(context.getString(R.string.rv_panel_edit_desc));
                textDialog.getCancelButton().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AppKeyboardUtil.hideInputMethodWindow(textDialog.getInputEditText().getContext(),textDialog.getInputEditText());
                        textDialog.dismiss();
                    }
                });
                textDialog.getOKButton().setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        AppKeyboardUtil.hideInputMethodWindow(textDialog.getInputEditText().getContext(),textDialog.getInputEditText());
                        textDialog.dismiss();
                        setDesc(getAdapterPosition(),textDialog.getInputEditText().getText().toString());

                    }
                });
                textDialog.show();
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        AppKeyboardUtil.showInputMethodWindow(textDialog.getInputEditText().getContext(),textDialog.getInputEditText());
                    }
                });
            }

            if (v.getId() == R.id.panel_more_tv_delete){
                if (list.get(getAdapterPosition()).getFlag() == FLAG_ANNOT){
                    delete(getAdapterPosition());
                    return;
                }
                delete(1,getAdapterPosition(),getAdapterPosition());
            }

            if (v.getId() == R.id.panel_more_tv_save){
                String fileName = list.get(getAdapterPosition()).getTitle();
                if (fileName == null || fileName.trim().length() < 1) {
                    AlertDialog dialog = new AlertDialog.Builder(((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity()).
                            setMessage("There was an error saving this document, Bad file name.").
                            setPositiveButton("Sure", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            }).create();
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.show();
                    return;
                }
                String suffix = fileName.substring(fileName.lastIndexOf(".")+1);
                UISaveAsDialog saveAsDialog = new UISaveAsDialog(((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity(), list.get(getAdapterPosition()).getTitle(), suffix, new ISaveAsOnOKClickCallBack() {
                    @Override
                    public void onOkClick(final String newFilePath) {
                        save(getAdapterPosition(),newFilePath);
                    }
                    @Override
                    public void onCancelClick() {

                    }
                });
                saveAsDialog.showDialog();
            }

            if (v.getId() == R.id.panel_attachment_container){
                String tempPath = Environment.getExternalStorageDirectory() + "/FoxitSDK/AttaTmp/";
                open(getAdapterPosition(),tempPath);

            }
        }

        @Override
        public void bind(BaseBean data) {

            FileBean item = (FileBean) data;
            icon.setImageResource(getIconResource(item.getTitle()));
            title.setText(item.getTitle());
            date_size.setText(item.getSize());
            desc.setText(item.getDesc());
            if (!AppUtil.isBlank(item.getDesc())) {
                desc.setVisibility(View.VISIBLE);
            }else {
                desc.setVisibility(View.GONE);
            }
            if(getAdapterPosition() != index) {
                view.setVisibility(View.GONE);
            }else {
                view.setVisibility(View.VISIBLE);
            }

            //3. when copy access.. == not allow: all can not save
            boolean enable = DocumentManager.getInstance(mPdfViewCtrl).canCopyForAssess() && DocumentManager.getInstance(mPdfViewCtrl).canCopy();
            more_save.setVisibility(enable?View.VISIBLE:View.GONE);
            //4.when extract == not allow: annot can save,but attachment can not;
            boolean annotEnable = (DocumentManager.getInstance(mPdfViewCtrl).canCopy() || item.getFlag() == FLAG_ANNOT)&&DocumentManager.getInstance(mPdfViewCtrl).canCopyForAssess();

            if (item.getFlag() == FLAG_ANNOT){
                //2.when comment ==not allow:annotation should not show more view! but fileattachment can!
                more.setEnabled(DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot());
                more.setClickable(DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot());
                more_save.setVisibility(annotEnable?View.VISIBLE:View.GONE);
            }else if (item.getFlag() == FLAG_NORMAL){
                //1.when modify ==not allow:fileattachment should not show more view! but annotation can!
                more.setEnabled(DocumentManager.getInstance(mPdfViewCtrl).canModifyContents());
                more.setClickable(DocumentManager.getInstance(mPdfViewCtrl).canModifyContents());

            }
        }

        private int getIconResource(String filename) {

            String ext = "";
            if (filename != null) {
                ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
            }

            if (ext.equals("pdf")) {
                return R.drawable.fb_file_pdf;
            } else if (ext.equals("ofd")) {
                return R.drawable.fb_file_ofd;
            } else if (ext.equals("ppdf")) {
                return R.drawable.fb_file_ppdf;
            } else if (ext.equals("png")) {
                return R.drawable.fb_file_png;
            } else if (ext.equals("jpg")) {
                return R.drawable.fb_file_jpg;
            } else if (ext.equals("doc")) {
                return R.drawable.fb_file_doc;
            } else if (ext.equals("txt")) {
                return R.drawable.fb_file_txt;
            } else if (ext.equals("xls")) {
                return R.drawable.fb_file_xls;
            } else if (ext.equals("ppt")) {
                return R.drawable.fb_file_ppt;
            } else {
                return R.drawable.fb_file_other;
            }
        }
    }

}
