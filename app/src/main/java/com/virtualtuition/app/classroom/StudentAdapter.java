package com.virtualtuition.app.classroom;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.virtualtuition.app.R;
import com.virtualtuition.app.models.UserProfile;
import com.virtualtuition.app.payment.AccessControlManager;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {

    private final Context context;
    private final List<UserProfile> studentList;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    public StudentAdapter(Context context) {
        this.context = context;
        this.studentList = new ArrayList<>();
    }

    public void setStudents(List<UserProfile> students) {
        this.studentList.clear();
        if (students != null) {
            this.studentList.addAll(students);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student, parent, false);
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        UserProfile student = studentList.get(position);

        holder.tvStudentName.setText(student.getName() != null ? student.getName() : "Student");
        holder.tvStudentEmail.setText(student.getEmail() != null ? student.getEmail() : "");

        // Compute feature decay and overdue days
        AccessControlManager.evaluateUserFeatures(student);
        int overdueDays = student.getOverdueDays();
        String status = student.getSubscriptionStatus();

        if (UserProfile.STATUS_ACTIVE.equalsIgnoreCase(status)) {
            holder.chipStudentPaymentStatus.setText("FEE PAID");
            holder.chipStudentPaymentStatus.setChipBackgroundColor(
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_active)));
        } else if (UserProfile.STATUS_GRACE_PERIOD.equalsIgnoreCase(status)) {
            holder.chipStudentPaymentStatus.setText("GRACE (DAY " + overdueDays + ")");
            holder.chipStudentPaymentStatus.setChipBackgroundColor(
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_grace)));
        } else if (UserProfile.STATUS_OVERDUE_PHASE1.equalsIgnoreCase(status)) {
            holder.chipStudentPaymentStatus.setText("VIDEO LOCKED (" + overdueDays + "d)");
            holder.chipStudentPaymentStatus.setChipBackgroundColor(
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_phase1)));
        } else if (UserProfile.STATUS_OVERDUE_PHASE2.equalsIgnoreCase(status)) {
            holder.chipStudentPaymentStatus.setText("ALL LOCKED (" + overdueDays + "d)");
            holder.chipStudentPaymentStatus.setChipBackgroundColor(
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_phase2)));
        } else {
            holder.chipStudentPaymentStatus.setText("SUSPENDED (" + overdueDays + "d)");
            holder.chipStudentPaymentStatus.setChipBackgroundColor(
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_suspended)));
        }

        StringBuilder subText = new StringBuilder();
        if (student.getNextDueDate() != null) {
            subText.append("Next Due: ").append(dateFormat.format(student.getNextDueDate().toDate()));
        } else {
            subText.append("No Due Date Recorded");
        }

        if (overdueDays > 0) {
            subText.append(" • ").append(overdueDays).append(" day(s) overdue");
        } else {
            subText.append(" • Up to date");
        }
        holder.tvStudentPaymentSub.setText(subText.toString());
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }

    static class StudentViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName, tvStudentEmail, tvStudentPaymentSub;
        Chip chipStudentPaymentStatus;

        public StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvStudentEmail = itemView.findViewById(R.id.tvStudentEmail);
            tvStudentPaymentSub = itemView.findViewById(R.id.tvStudentPaymentSub);
            chipStudentPaymentStatus = itemView.findViewById(R.id.chipStudentPaymentStatus);
        }
    }
}
