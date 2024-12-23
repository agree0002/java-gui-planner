import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.io.*;
import java.util.List;

public class CampusPlanner extends JFrame {
    private Container contentPane;
    private String[] columnNames = {"Date", "Schedule", "Classification"};
    JPanel scheduleFrame = new JPanel();
    JTable table;
    private ScheduleAddDialog scheduleAddDialog;
    DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0);
    TableRowSorter<DefaultTableModel> sorter;
    File file = new File("schedule.txt");
    private JTextField scheduleInputTf;
    private JTextField[]dateInputTf = new JTextField[3];
    JToolBar toolBar = new JToolBar();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    boolean cancelAddingSchedule = false;

    public CampusPlanner() {
        setTitle("Planner");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        contentPane = getContentPane();

        table = new ScheduleTable();
        JScrollPane scrollPane = new JScrollPane(table);
        scheduleFrame.add(scrollPane);
        contentPane.add(scheduleFrame, BorderLayout.CENTER);

        scheduleAddDialog = new ScheduleAddDialog(this, "add schedule");
        createMenu();
        createToolBar();
        loadSchedules();

        setSize(1000, 650);
        setVisible(true);
    }


    private void createClassificationRadioButton() {
        JRadioButton[]radio = new JRadioButton[5];
        String[]radioText = {"all", "exam", "assignment", "contest", "others"};

        ButtonGroup g = new ButtonGroup();
        for (int i = 0; i < radio.length; i++) {
            radio[i] = new JRadioButton(radioText[i]);
            g.add(radio[i]);
            toolBar.add(radio[i]);
            radio[i].addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.DESELECTED)
                        return;
                    for (int i = 0; i < radio.length; i++) {
                        if (radio[0].isSelected()) {
                            sorter.setRowFilter(null);
                            break;
                        }
                        else if (radio[i].isSelected()) {
                            sorter.setRowFilter(RowFilter.regexFilter(radioText[i], 2));
                            break;
                        }
                    }
                }
            });
        }
        radio[0].setSelected(true);
    }


    private void createToolBar() {
        JButton addButton = new ScheduleAddButton();
        JButton deleteButton = new ScheduleDeleteButton();
        String date = LocalDate.now().format(formatter);
        JLabel toDayLabel = new JLabel("Today : " + date);

        toolBar.setPreferredSize(new Dimension(800, 50));
        toolBar.add(Box.createHorizontalStrut(50));
        toolBar.add(toDayLabel);
        toolBar.add(Box.createHorizontalStrut(100));
        toolBar.add(addButton);
        toolBar.add(Box.createHorizontalStrut(50));
        toolBar.add(deleteButton);
        toolBar.add(Box.createHorizontalStrut(30));
        toolBar.addSeparator();
        toolBar.add(Box.createHorizontalStrut(30));

        createClassificationRadioButton();

        toolBar.setFloatable(false);
        contentPane.add(toolBar, BorderLayout.NORTH);
        toolBar.setOpaque(false);
    }


    private void saveSchedules() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                for (int j = 0; j < tableModel.getColumnCount(); j++) {
                    writer.write((String)tableModel.getValueAt(i, j));
                    if (j < tableModel.getColumnCount() - 1) {
                        writer.write(", ");
                    }
                }
                writer.write("\n");
            }
        }
        catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving schedules");
        }
    }


    private void loadSchedules() {
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String str;
            while ((str = reader.readLine()) != null) {
                String[] data = str.split(", ");
                tableModel.addRow(data);
            }
        }
        catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error loading schedules");
        }
    }


    private void createMenu() {
        JMenuBar mb = new JMenuBar();
        JMenu modeMenu = new JMenu("Mode");
        JMenuItem dateItem = new JMenuItem("Date");
        JMenuItem ddayItem = new JMenuItem("D-Day");
        dateItem.addActionListener(new MenuActionListener());
        ddayItem.addActionListener(new MenuActionListener());
        modeMenu.add(dateItem);
        modeMenu.add(ddayItem);

        mb.add(modeMenu);
        setJMenuBar(mb);
    }


    class MenuActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String cmd = e.getActionCommand();
            switch (cmd) {
                case "Date":
                    table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer());
                    table.repaint();
                    break;
                case "D-Day":
                    table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
                        @Override
                        protected void setValue(Object value) {
                            if (value instanceof String) {
                                LocalDate scheduleDate = LocalDate.parse((String) value);
                                LocalDate today = LocalDate.now();
                                long dday = ChronoUnit.DAYS.between(today, scheduleDate);

                                if (dday > 0) {
                                    setText("D-" + dday);
                                }
                                else if (dday < 0) {
                                    setText("D+" + Math.abs(dday));
                                }
                                else {
                                    setText("D-Day");
                                }
                            }
                            else {
                                super.setValue(value);
                            }
                        }
                    });
                    table.repaint();
                    break;
            }
        }
    }


    class ScheduleTable extends JTable {
        public ScheduleTable() {
            super(tableModel);
            setPreferredScrollableViewportSize(new Dimension(800, 500));
            getColumnModel().getColumn(0).setPreferredWidth(150);
            getColumnModel().getColumn(1).setPreferredWidth(500);
            getColumnModel().getColumn(2).setPreferredWidth(150);
            setRowHeight(50);
            setFont(new Font("SansSerif", Font.PLAIN, 15));

            sorter = new TableRowSorter<>(tableModel);
            setRowSorter(sorter);
            sorter.setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.ASCENDING)));
        }
    }


    class ScheduleAddButton extends JButton {
        public ScheduleAddButton() {
            super("Add");
            addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    scheduleAddDialog.setVisible(true);
                    Schedule schedule = scheduleAddDialog.getInput();
                    if (schedule != null) {
                        String date = schedule.getDate().format(formatter);
                        tableModel.addRow(new Object[]{date, schedule.getToDo(), schedule.getClassification()});
                        saveSchedules();
                    }
                    else {
                        if (!cancelAddingSchedule) {
                            JOptionPane.showMessageDialog(null, "Please enter a valid date.",
                                    "Message", JOptionPane.ERROR_MESSAGE);
                        }
                        else {
                            cancelAddingSchedule = false;
                        }
                    }

                    for (int i = 0; i < 3; i++) {
                        dateInputTf[i].setText("");
                    }
                    scheduleInputTf.setText("");
                }
            });
        }
    }


    class ScheduleDeleteButton extends JButton {
        public ScheduleDeleteButton() {
            super("Delete");
            addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int selectedRow = table.getSelectedRow();
                    if (selectedRow != -1) {
                        int result = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete it?",
                                "Confirm", JOptionPane.YES_NO_OPTION);
                        if (result == JOptionPane.YES_OPTION) {
                            int modelRow = table.convertRowIndexToModel(selectedRow);
                            tableModel.removeRow(modelRow);
                            saveSchedules();
                        }
                    }
                }
            });
        }
    }


    class ScheduleAddDialog extends JDialog {
        private JButton okButton = new JButton("OK");
        private String[]labels = {"schedule", "Classification"};
        private String[]classifications = {"exam", "assignment", "contest", "others"};
        private JComboBox<String>strCombo = new JComboBox<String>(classifications);

        private JButton cancelButton = new JButton("Cancel");

        public ScheduleAddDialog(JFrame frame, String title) {
            super(frame, title, true);
            setLayout(null);

            for (int i = 0; i < 2; i++) {
                JLabel la = new JLabel(labels[i]);
                la.setHorizontalAlignment(JLabel.RIGHT);
                la.setSize(100, 20);
                la.setLocation(60, 75 + i * 25);
                add(la);
            }

            JLabel yearLa = new JLabel("year");
            yearLa.setSize(50, 20);
            yearLa.setLocation(135, 50);
            add(yearLa);

            JLabel monthLa = new JLabel("month");
            monthLa.setSize(50, 20);
            monthLa.setLocation(220, 50);
            add(monthLa);

            JLabel dayLa = new JLabel("day");
            dayLa.setSize(50, 20);
            dayLa.setLocation(340, 50);
            add(dayLa);

            for (int i = 0; i < 3; i++) {
                dateInputTf[i] = new JTextField(10);
                dateInputTf[i].setHorizontalAlignment(JTextField.CENTER);
                dateInputTf[i].setSize(50, 20);
                dateInputTf[i].setLocation(160 + i * 100, 50);
                add(dateInputTf[i]);
            }

            scheduleInputTf = new JTextField(30);
            scheduleInputTf.setHorizontalAlignment(JTextField.CENTER);
            scheduleInputTf.setSize(250, 20);
            scheduleInputTf.setLocation(160, 75);
            add(scheduleInputTf);

            strCombo.setSize(250, 20);
            strCombo.setLocation(160, 100);
            add(strCombo);

            okButton.setSize(60, 30);
            okButton.setLocation(200, 125);
            add(okButton);

            cancelButton.setSize(100, 30);
            cancelButton.setLocation(250, 125);
            add(cancelButton);
            setSize(500, 200);

            okButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setVisible(false);
                }
            });

            cancelButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    cancelAddingSchedule = true;
                    setVisible(false);
                }
            });
        }

        public Schedule getInput() {
            if (cancelAddingSchedule) {
                return null;
            }
            try {
                int year = Integer.parseInt(dateInputTf[0].getText());
                int month = Integer.parseInt(dateInputTf[1].getText());
                int day = Integer.parseInt(dateInputTf[2].getText());
                LocalDate localDate = LocalDate.of(year, month, day);

                Schedule schedule = new Schedule(localDate, scheduleInputTf.getText(), (String) strCombo.getSelectedItem());
                return schedule;
            }
            catch (NumberFormatException e) {
                return null;
            }
        }
    }


    class Schedule {
        private LocalDate date;
        private String ToDo;
        private String classification;

        public Schedule() {
        }

        public Schedule(LocalDate date, String toDo, String classification) {
            this.date = date;
            ToDo = toDo;
            this.classification = classification;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public String getToDo() {
            return ToDo;
        }

        public void setToDo(String toDo) {
            ToDo = toDo;
        }

        public String getClassification() {
            return classification;
        }

        public void setClassification(String classification) {
            this.classification = classification;
        }
    }


    public static void main(String[] args) {
        new CampusPlanner();
    }
}