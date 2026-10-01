package com.virtualtuition.app.dashboard;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.virtualtuition.app.R;
import com.virtualtuition.app.models.Classroom;
import java.util.ArrayList;
import java.util.List;

public class ClassroomAdapter extends RecyclerView.Adapter<ClassroomAdapter.ClassroomViewHolder> {

    public interface OnClassroomClickListener {
        void onClassroomClick(Classroom classroom);
        void onLiveClassClick(Classroom classroom);
    }

    private final Context context;
    private final List<Classroom> classroomList;
    private final OnClassroomClickListener listener;

    public ClassroomAdapter(Context context, OnClassroomClickListener listener) {
        this.context = context;
        this.classroomList = new ArrayList<>();
        this.listener = listener;
    }

    public void setClassrooms(List<Classroom> classrooms) {
        this.classroomList.clear();
        if (classrooms != null) {
            this.classroomList.addAll(classrooms);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ClassroomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_classroom, parent, false);
        return new ClassroomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClassroomViewHolder holder, int position) {
        Classroom classroom = classroomList.get(position);
        holder.tvClassName.setText(classroom.getClassName());
        holder.tvClassSubject.setText(classroom.getSubject());
        holder.tvTeacherName.setText("Teacher: " + classroom.getTeacherName());
        holder.chipClassCode.setText("CODE: " + classroom.getClassCode());

        View.OnClickListener copyListener = v -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Class Code", classroom.getClassCode());
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, "Class code " + classroom.getClassCode() + " copied to clipboard!", Toast.LENGTH_SHORT).show();
            }
        };

        holder.chipClassCode.setOnClickListener(copyListener);
        if (holder.btnCopyCode != null) {
            holder.btnCopyCode.setOnClickListener(copyListener);
        }

        holder.btnOpenClassroom.setOnClickListener(v -> {
            if (listener != null) listener.onClassroomClick(classroom);
        });

        holder.btnQuickLive.setOnClickListener(v -> {
            if (listener != null) listener.onLiveClassClick(classroom);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClassroomClick(classroom);
        });
    }

    @Override
    public int getItemCount() {
        return classroomList.size();
    }

    static class ClassroomViewHolder extends RecyclerView.ViewHolder {
        TextView tvClassName, tvClassSubject, tvTeacherName;
        Chip chipClassCode;
        ImageButton btnCopyCode;
        MaterialButton btnQuickLive, btnOpenClassroom;

        public ClassroomViewHolder(@NonNull View itemView) {
            super(itemView);
            tvClassName = itemView.findViewById(R.id.tvClassName);
            tvClassSubject = itemView.findViewById(R.id.tvClassSubject);
            tvTeacherName = itemView.findViewById(R.id.tvTeacherName);
            chipClassCode = itemView.findViewById(R.id.chipClassCode);
            btnCopyCode = itemView.findViewById(R.id.btnCopyCode);
            btnQuickLive = itemView.findViewById(R.id.btnQuickLive);
            btnOpenClassroom = itemView.findViewById(R.id.btnOpenClassroom);
        }
    }
}
