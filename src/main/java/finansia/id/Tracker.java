package finansia.id;

import com.formdev.flatlaf.FlatDarkLaf;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jdatepicker.impl.*;
import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import javax.swing.table.DefaultTableModel;

public class Tracker extends JFrame {

    private final DefaultTableModel tableModel; // Model tabel untuk menyimpan data transaksi
    private final JTable table; // Komponen tabel untuk menampilkan data
    private final JDatePickerImpl datePicker; // Komponen pemilihan tanggal
    private final JTextField descriptionField; // Field input untuk deskripsi transaksi
    private final JTextField amountField; // Field input untuk jumlah transaksi
    private final JComboBox<String> typeCombobox; // Dropdown untuk memilih tipe transaksi (Pemasukan/Pengeluaran)
    private final JButton addButton; // Tombol untuk menambahkan entri
    private final JButton editButton; // Tombol untuk mengedit entri
    private final JButton removeButton; // Tombol untuk menghapus entri
    private final JButton showChartButton; // Tombol untuk menampilkan/menyembunyikan chart
    private final JButton setTargetButton; // Tombol untuk menetapkan target saldo
    private final JLabel balanceLabel; // Label untuk menampilkan saldo saat ini
    private final JProgressBar progressBar; // Progress bar untuk menunjukkan pencapaian target saldo
    private int balance; // Variabel untuk menyimpan saldo saat ini
    private int targetBalance; // Variabel untuk menyimpan target saldo
    private final NumberFormat currencyFormat; // Format untuk menampilkan angka dalam mata uang
    private final DefaultCategoryDataset dataset; // Dataset untuk grafik saldo
    private final ChartPanel chartPanel; // Panel untuk menampilkan grafik
    private Date targetDate; // Variabel Target Tanggal

    public Tracker() {
        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID")); // Mengatur format mata uang ke format Indonesia

        dataset = new DefaultCategoryDataset();

        tableModel = new DefaultTableModel(new String[]{"Tanggal", "Deskripsi", "Jumlah", "Tipe"}, 0);
        table = new JTable(tableModel); // Membuat tabel menggunakan model tabel

        //Mengatur komponen pemilihan tanggal dengan model, panel, dan formatter.
        UtilDateModel model = new UtilDateModel();
        Properties p = new Properties();
        p.put("text.today", "Today");
        p.put("text.month", "Month");
        p.put("text.year", "Year");
        JDatePanelImpl datePanel = new JDatePanelImpl(model, p);
        datePicker = new JDatePickerImpl(datePanel, new DateLabelFormatter());

        //Menginisiasi dan mengatur variabel input data.
        descriptionField = new JTextField(20);
        amountField = new JTextField(10);
        typeCombobox = new JComboBox<>(new String[]{"Pengeluaran", "Pemasukan"});
        addButton = new JButton("Tambah");
        editButton = new JButton("Edit");
        removeButton = new JButton("Hapus");
        showChartButton = new JButton("Tampilkan Grafik");
        setTargetButton = new JButton("Set Target");
        balanceLabel = new JLabel("Saldo: " + formatBalance(balance));
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);

        //Menambahkan event listener untuk setiap tombol dalam aplikasi.
        addButton.addActionListener(e -> addEntry());
        editButton.addActionListener(e -> editEntry());
        removeButton.addActionListener(e -> removeEntry());
        showChartButton.addActionListener(e -> toggleChartVisibility());
        setTargetButton.addActionListener(e -> setTarget());

        //Mengatur tata letak panel input dan panel bawah untuk menampilkan UI seperti tabel dan tombol.
        JPanel inputPanel = new JPanel();
        inputPanel.add(new JLabel("Tanggal"));
        inputPanel.add(datePicker);

        inputPanel.add(new JLabel("Deskripsi"));
        inputPanel.add(descriptionField);

        inputPanel.add(new JLabel("Jumlah"));
        inputPanel.add(amountField);

        inputPanel.add(new JLabel("Tipe"));
        inputPanel.add(typeCombobox);

        inputPanel.add(addButton);
        inputPanel.add(editButton);
        inputPanel.add(removeButton);
        inputPanel.add(showChartButton);
        inputPanel.add(setTargetButton);

        // Membuat panel bawah dengan tata letak BorderLayout
        JPanel bottomPanel = new JPanel(new BorderLayout());

        // Panel kanan bawah untuk progres bar dan saldo
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.add(progressBar);
        rightPanel.add(balanceLabel);

        // Menambahkan panel kiri dan kanan ke panel bawah
        bottomPanel.add(rightPanel, BorderLayout.EAST);

        //Membuat grafik saldo mengatur komposisi serta interaksi grafik.
        JFreeChart chart = ChartFactory.createLineChart(
                "Perubahan Saldo",
                "Waktu",
                "Saldo",
                dataset
        );

        //Mengaktifkan dan mengatur tampilan garis dan bentuk chart.
        chart.getCategoryPlot().setRangePannable(true);
        LineAndShapeRenderer renderer = new LineAndShapeRenderer();
        renderer.setSeriesShapesVisible(0, true);
        renderer.setSeriesLinesVisible(0, true);
        renderer.setDefaultToolTipGenerator(new StandardCategoryToolTipGenerator());

        //Konfigurasi chart baru.
        chart.getCategoryPlot().setRenderer(new org.jfree.chart.renderer.category.LineAndShapeRenderer(true, false));

        //Membuat panel chart, dan mengatur ukuran serta menyembunyikannya secara default.
        chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(400, 300));
        chartPanel.setVisible(false);

        //Mengatur layouting JFrame
        setLayout(new BorderLayout());

        add(inputPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(chartPanel, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        //Mengatur properti Jframe
        setTitle("Finansia");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    //Void untuk menambahkan data pada kolom tabel, memvalidasi data dan memperbarui tabel
    private void addEntry() {
        String date = datePicker.getJFormattedTextField().getText();
        String description = descriptionField.getText();
        String amountStr = amountField.getText();
        String type = (String) typeCombobox.getSelectedItem();
        double amount;

        if (amountStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Masukkan Jumlah", "Kesalahan", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (date == null || date.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Masukkan Tanggal", "Kesalahan", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (descriptionField == null || descriptionField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Masukkan Deskripsi", "Kesalahan", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Format Jumlah Tidak Valid", "Kesalahan", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (type.equals("Pemasukan") && amount < 0) {
            JOptionPane.showMessageDialog(this, "Jumlah untuk tipe Pemasukan tidak boleh negatif.", "Kesalahan", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (type.equals("Pengeluaran")) {
            amount *= -1;
        }

        Test entry = new Test(date, description, amount, type);
        addEntryToTable(entry);

        balance += amount;
        balanceLabel.setText("Saldo: " + formatBalance(balance));

        updateChart(date, balance);
        updateProgressBar();
        clearInputFields();
    }

    //Menambahkan entri pada tabel.
    private void addEntryToTable(Test entry) {
        tableModel.addRow(new Object[]{entry.getDate(), entry.getDescription(), entry.getAmount(), entry.getType()});
    }

    //Mengedit atau Memperbarui, memvalidasi dan menghitung ulang saldo berdasarkan jumlah data pada tabel.
    private void editEntry() {
        int selectedRowIndex = table.getSelectedRow();
        if (selectedRowIndex == -1) {
            JOptionPane.showMessageDialog(this, "Pilih baris untuk diedit", "Kesalahan", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String updatedDate = datePicker.getJFormattedTextField().getText();
        String updatedDescription = descriptionField.getText();
        String updatedAmountStr = amountField.getText();
        String updatedType = (String) typeCombobox.getSelectedItem();

        if (updatedAmountStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Masukkan Jumlah yang Diperbarui", "Kesalahan", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (updatedDate == null || updatedDate.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Masukkan Tanggal yang Diperbaharui", "Kesalahan", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (updatedDescription == null || updatedDescription.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Masukkan Deskripsi yang Diperbaharui", "Kesalahan", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            double updatedAmount = Double.parseDouble(updatedAmountStr);
            if (updatedType.equals("Pengeluaran")) {
                updatedAmount *= -1;
            }

            Test updatedEntry = new Test(updatedDate, updatedDescription, updatedAmount, updatedType);
            double previousAmount = (double) tableModel.getValueAt(selectedRowIndex, 2);

            tableModel.setValueAt(updatedEntry.getDate(), selectedRowIndex, 0);
            tableModel.setValueAt(updatedEntry.getDescription(), selectedRowIndex, 1);
            tableModel.setValueAt(updatedEntry.getAmount(), selectedRowIndex, 2);
            tableModel.setValueAt(updatedEntry.getType(), selectedRowIndex, 3);

            balance += updatedAmount - previousAmount;
            balanceLabel.setText("Saldo: " + formatBalance(balance));

            updateChart(updatedDate, balance);
            updateProgressBar();
            clearInputFields();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Format Jumlah yang Diperbarui Tidak Valid", "Kesalahan", JOptionPane.ERROR_MESSAGE);
        }

    }

    //Menghapus data pada tabel.
    private void removeEntry() {
        int selectedRowIndex = table.getSelectedRow();
        if (selectedRowIndex == -1) {
            JOptionPane.showMessageDialog(this, "Pilih baris untuk dihapus", "Kesalahan", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (tableModel.getRowCount() == 0 ) {
            dataset.clear();
        }

        double removedAmount = (double) table.getValueAt(selectedRowIndex, 2);
        String date = (String) table.getValueAt(selectedRowIndex, 0);
        tableModel.removeRow(selectedRowIndex);

        balance -= removedAmount;
        balanceLabel.setText("Saldo: " + formatBalance(balance));

        updateChart(date, balance);
        updateProgressBar();
    }

    //Memperbarui Chart.
    private void updateChart(String date, double updatedBalance) {
        dataset.addValue(updatedBalance, "Saldo", date);

        if (dataset.getColumnCount() > 1 || !date.isEmpty()) {
            dataset.addValue(updatedBalance, "Saldo", date);
            chartPanel.revalidate();
            chartPanel.repaint();
        }
    }

    //Memperbarui Progress Bar.
    private void updateProgressBar() {
        if (targetBalance > 0 && targetDate != null) {
            int progress = (int) ((double) balance / targetBalance * 100);
            progressBar.setValue(Math.min(progress, 100));

            Date currentDate = new Date();

            if (balance >= targetBalance) {
                if (currentDate.compareTo(targetDate) <= 0) {
                    playsuara();
                    JOptionPane.showMessageDialog(this, "Selamat! Anda telah mencapai target saldo sebelum atau tepat waktu!", "Target Tercapai", JOptionPane.INFORMATION_MESSAGE);
                }

                int option = JOptionPane.showConfirmDialog(this,
                        "Apakah Anda ingin menetapkan target baru?",
                        "Target Baru",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);

                if (option == JOptionPane.YES_OPTION) {
                    setTarget();
                } else {
                    targetBalance = 0;
                    targetDate = null;
                    progressBar.setValue(0);
                }
            } else if (currentDate.compareTo(targetDate) > 0) {
                JOptionPane.showMessageDialog(this, "Maaf, Anda tidak mencapai target saldo hingga tanggal yang ditentukan.", "Target Gagal", JOptionPane.WARNING_MESSAGE);
                playlose();
                targetBalance = 0;
                targetDate = null;
                progressBar.setValue(0);
            }
        }
    }

    //Mengatur target tanggal terpenuhinya target saldo user.
    private void setTarget() {
        JPanel panel = new JPanel(new GridLayout(2, 2));

        JLabel targetAmountLabel = new JLabel("Masukkan target saldo Anda:");
        JTextField targetAmountField = new JTextField();

        JLabel targetDateLabel = new JLabel("Pilih tanggal target Anda:");
        UtilDateModel model = new UtilDateModel();
        Properties p = new Properties();
        p.put("text.today", "Today");
        p.put("text.month", "Month");
        p.put("text.year", "Year");
        JDatePanelImpl datePanel = new JDatePanelImpl(model, p);
        JDatePickerImpl targetDatePicker = new JDatePickerImpl(datePanel, new DateLabelFormatter());

        panel.add(targetAmountLabel);
        panel.add(targetAmountField);
        panel.add(targetDateLabel);
        panel.add(targetDatePicker);

        int result = JOptionPane.showConfirmDialog(this, panel, "Set Target", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            String targetInput = targetAmountField.getText();
            String targetDateInput = targetDatePicker.getJFormattedTextField().getText();

            if (targetInput.isEmpty() || targetDateInput.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Semua input harus diisi!", "Kesalahan", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                targetBalance = Integer.parseInt(targetInput);
                targetDate = new SimpleDateFormat("dd-MM-yyyy").parse(targetDateInput);

                playklik();
                updateProgressBar();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Format target saldo tidak valid!", "Kesalahan", JOptionPane.ERROR_MESSAGE);
            } catch (ParseException ex) {
                JOptionPane.showMessageDialog(this, "Format tanggal tidak valid!", "Kesalahan", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    //Void untuk memutar sound effect klik pada pengaturan target saldo.
    private void playklik() {
        try {
            URL url = this.getClass().getClassLoader().getResource("click-47609.wav");
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.start();
        } catch (UnsupportedAudioFileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    //Void untuk memutar sound effect klik pada pengaturan target saldo.
    private void playlose() {
        try {
            URL url = this.getClass().getClassLoader().getResource("marimba-lose-250960.wav");
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.start();
        } catch (UnsupportedAudioFileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    //Membersihkan semua input pada form.
    private void clearInputFields() {
        datePicker.getJFormattedTextField().setText("");
        descriptionField.setText("");
        amountField.setText("");
        typeCombobox.setSelectedIndex(0);
    }

    //Pengaturan perubah visibilitas chart.
    private void toggleChartVisibility() {
        chartPanel.setVisible(!chartPanel.isVisible());
    }

    //Void untuk memutar sound effect pada pop up selamat.
    private void playsuara() {
        try {
            URL url = this.getClass().getClassLoader().getResource("congratulations.wav");
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.start();
        } catch (UnsupportedAudioFileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }

    }

    //Pemformat variabel balance menjadi mmata uang Rupiah.
    private String formatBalance(double balance) {
        return currencyFormat.format(balance);
    }

    //Fungsi utama aplikasi dijalankan serta penerapan dark theme.
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatDarkLaf.setup();
            new Tracker();
        });
    }
}

    //Penyimpanan data tabungan.
class Test {
    private final String date;
    private final String description;
    private final double amount;
    private final String type;

    public Test (String date, String description, double amount, String type) {
        this.date = date;
        this.description = description;
        this.amount = amount;
        this.type = type;
    }

    public String getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }

    public double getAmount() {
        return amount;
    }

    public String getType() {
        return type;
    }
}

//Formatting tanggal agar memiliki "-" sebagai pemisah.
class DateLabelFormatter extends JFormattedTextField.AbstractFormatter {
    private final String datePattern = "dd-MM-yyyy";
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat(datePattern);

    @Override
    public Object stringToValue(String text) throws ParseException {
        return dateFormatter.parse(text);
    }

    @Override
    public String valueToString(Object value) throws ParseException {
        if (value != null) {
            Calendar cal = (Calendar) value;
            return dateFormatter.format(cal.getTime());
        }
        return "";
    }
}

