package com.virtualtuition.app.classroom;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.virtualtuition.app.R;
import com.virtualtuition.app.models.MaterialItem;
import com.virtualtuition.app.models.UserProfile;
import com.virtualtuition.app.payment.AccessControlManager;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MaterialAdapter extends RecyclerView.Adapter<MaterialAdapter.MaterialViewHolder> {

    private final Context context;
    private final List<MaterialItem> materialList;
    private final UserProfile currentUser;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

    public MaterialAdapter(Context context, UserProfile currentUser) {
        this.context = context;
        this.currentUser = currentUser;
        this.materialList = new ArrayList<>();
    }

    public void setMaterials(List<MaterialItem> items) {
        this.materialList.clear();
        if (items != null) {
            this.materialList.addAll(items);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MaterialViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_material, parent, false);
        return new MaterialViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MaterialViewHolder holder, int position) {
        MaterialItem item = materialList.get(position);

        holder.tvMaterialTitle.setText(item.getTitle());
        holder.tvMaterialDesc.setText(item.getDescription());
        holder.tvFileName.setText(item.getFileName());

        String dateStr = item.getTimestamp() != null ? dateFormat.format(item.getTimestamp().toDate()) : "";
        holder.tvMaterialAuthorDate.setText("Posted by " + item.getUploadedByName() + " • " + dateStr);

        // Icon based on file type
        if ("image".equalsIgnoreCase(item.getFileType())) {
            holder.ivFileType.setImageResource(R.drawable.ic_materials);
            holder.ivImagePreview.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(item.getFileUrl())
                    .placeholder(R.drawable.ic_materials)
                    .into(holder.ivImagePreview);
        } else {
            holder.ivImagePreview.setVisibility(View.GONE);
            holder.ivFileType.setImageResource(R.drawable.ic_materials);
        }

        // Preview Material
        holder.btnPreviewMaterial.setOnClickListener(v -> {
            if (!AccessControlManager.isMaterialsAllowed(currentUser)) {
                AccessControlManager.showLockedFeatureDialog(
                        context,
                        "Material Preview",
                        "Study material previews and downloads are locked due to overdue tuition fees (Phase 2 lock). Please pay your fee to access materials."
                );
                return;
            }

            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(item.getFileUrl()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(context, "No app available to open this file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Download Material
        holder.btnDownloadMaterial.setOnClickListener(v -> {
            if (!AccessControlManager.isMaterialsAllowed(currentUser)) {
                AccessControlManager.showLockedFeatureDialog(
                        context,
                        "Material Downloads",
                        "Study material downloads are locked due to overdue tuition fees (Phase 2 lock). Please pay your fee to download materials."
                );
                return;
            }

            downloadFile(item);
        });
    }

    private void downloadFile(MaterialItem item) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(item.getFileUrl()));
            request.setTitle(item.getFileName());
            request.setDescription("Downloading classroom study material...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, item.getFileName());

            DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (manager != null) {
                manager.enqueue(request);
                Toast.makeText(context, "Download started for: " + item.getFileName(), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(context, "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return materialList.size();
    }

    static class MaterialViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFileType, ivImagePreview;
        TextView tvMaterialTitle, tvMaterialAuthorDate, tvMaterialDesc, tvFileName;
        MaterialButton btnPreviewMaterial, btnDownloadMaterial;

        public MaterialViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFileType = itemView.findViewById(R.id.ivFileType);
            ivImagePreview = itemView.findViewById(R.id.ivImagePreview);
            tvMaterialTitle = itemView.findViewById(R.id.tvMaterialTitle);
            tvMaterialAuthorDate = itemView.findViewById(R.id.tvMaterialAuthorDate);
            tvMaterialDesc = itemView.findViewById(R.id.tvMaterialDesc);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            btnPreviewMaterial = itemView.findViewById(R.id.btnPreviewMaterial);
            btnDownloadMaterial = itemView.findViewById(R.id.btnDownloadMaterial);
        }
    }
}
