package view;

import control.Controller;
import error.SimulationError;
import event.Event;
import logic.*;
import util.ReportsPanelStream;
import view.dialog.ReportsDialog;
import view.mapcomponent.MapComponent;
import view.menubar.MenuBar;
import view.observer.ObserverTrafficSimulator;
import view.TableModel.EventsTableModel;
import view.TableModel.JunctionsTableModel;
import view.TableModel.RoadsTableModel;
import view.TableModel.VehiclesTableModel;
import view.panel.*;
import view.toolbar.ToolBar;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Scanner;

public class MainWindow extends JFrame implements ObserverTrafficSimulator {

    public static Border defaultBorder = BorderFactory.createLineBorder(Color.black, 2);

    // SUPERIOR PANEL
    static private final String[] columnIdEvents = {"#", "Tiempo", "Tipo"};

    private TextAreaPanel panelEventsEditor;
    private TextAreaPanel panelReports;
    private TablePanel<Event> panelEventsQueue;

    // MENU AND TOOL BAR
    private JFileChooser fileChooser;
    private ToolBar toolBar;
    private MenuBar menuBar;

    // GRAPHIC PANEL
    private MapComponent mapComponent;

    // STATUS BAR (INFO AT THE BOTTON OF THE WINDOW)
    private StatusBarPanel panelStatusBar;

    // INFERIOR PANEL
    static private final String[] columnIdVehicle = {"ID", "Road", "Location", "Speed", "Km.", "Brdwn. Time", "Itinerary"};
    static private final String[] columnIdRoad = {"ID", "Origin", "Destination", "Length", "Max. Speed", "Vehicles"};
    static private final String[] columnIdJunction = {"ID", "Green", "Red"};

    private TablePanel<Vehicle> panelVehicles;
    private TablePanel<Road> panelRoads;
    private TablePanel<GenericJunction<?>> panelJunctions;

    // REPORT DIALOG
    private ReportsDialog reportsDialog;

    // BRVEHICLE DIALOG
	private BrVehiclesDialog brVehiclesDialog;

	// REMOVE VEHICLES DIALOG
	private RemoveVehiclesDialog removeVehiclesDialog;

    // MODEL PART - VIEW CONTROLLER MODEL
    private File actualFile;
    private Controller controller;

    // OUTPUT OPTIONS
	public enum OutputOption{CONSOLE, GRAPHIC}

	// THREAD FOR EXECUTE THE SIMULATION
	private Thread control_execute_thread;

    public MainWindow(String inputFile, Controller controller) throws SimulationError {
        super("Traffic Simulator");
        this.controller = controller;
        this.actualFile = (inputFile != null ? new File(inputFile) : null);
        this.initGUI();
        controller.addObserver(this);
    }

    private void initGUI() throws SimulationError {
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowListener() {
            @Override
            public void windowOpened(WindowEvent e) {
				// empty, its not necessary to be implemented
            }

            @Override
            public void windowClosing(WindowEvent e) {
                exit();
            }

            @Override
            public void windowClosed(WindowEvent e) {
				// empty, its not necessary to be implemented
            }

            @Override
            public void windowIconified(WindowEvent e) {
				// empty, its not necessary to be implemented
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
				// empty, its not necessary to be implemented
            }

            @Override
            public void windowActivated(WindowEvent e) {
				// empty, its not necessary to be implemented
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
				// empty, its not necessary to be implemented
            }
        });
        JPanel mainPanel = this.createMainPanel();
        this.setContentPane(mainPanel);

        // BARRA DE HERRAMIENTAS
        this.addToolBar(mainPanel);

        // PANEL QUE CONTIENE EL RESTO DE COMPONENTES
        // (Lo dividimos en dos paneles (superior e inferior))
        JPanel centralPanel = this.createCentralPanel();
        mainPanel.add(centralPanel, BorderLayout.CENTER);

        // PANEL SUPERIOR
        this.createTopPanel(centralPanel);

        // MENU
        this.menuBar = new MenuBar(this, this.controller);
        this.setJMenuBar(menuBar);

        // PANEL INFERIOR
        this.createBottomPanel(centralPanel);

        // BARRA ESTADO INFERIOR
        // (contiene una JLabel param mostrar el estado del simulador)
        this.addStatusBar(mainPanel);

        // FILE CHOOSER
        this.fileChooser = new JFileChooser();

        // REPORT DIALOG
        this.reportsDialog = new ReportsDialog(this, this.controller);

        // BRVEHICLE DIALOG
		this.brVehiclesDialog = new BrVehiclesDialog(this, this.controller);

		// REMOVE VEHICLES DIALOG
		this.removeVehiclesDialog = new RemoveVehiclesDialog(this,this.controller);

        // MAINWINDOW FRAME CONFIG
        this.setVisibleReportsDialog(false);
		this.setMinimumSize(new Dimension(1920, 1080));
		this.setPreferredSize(new Dimension(2240, 1720)); // Comentar para entregar
		this.switchOutputStream(OutputOption.GRAPHIC); // Opcion grafica para salida reportes por defecto
		this.pack();
        this.setVisible(true);
    }

    private JPanel createCentralPanel(){
        JPanel centralPanel = new JPanel();
        // Para colocar topPanel y bottomPanel
        centralPanel.setLayout(new GridLayout(2,1));
        return centralPanel;
    }

    private void createTopPanel(JPanel centralPanel){
        JPanel topPanel = new JPanel();
        String text = "", inputName = (this.actualFile != null ? this.actualFile.getName() : "none" );
        try{
            text = ( this.actualFile != null ? this.readFile(this.actualFile) : "");
        }catch(FileNotFoundException e){
            this.actualFile = null;
            this.showErrorDialog("ERROR: file read did not work " + e.getMessage());
        }
        topPanel.setLayout(new BoxLayout( topPanel, BoxLayout.X_AXIS)); // pendiente revision
        // No es observador
        this.panelEventsEditor = new EventsEditorPanel(inputName, text, true,this);
        // Es observador
        this.panelEventsQueue = new TablePanel<Event>("Events Queue: ", new EventsTableModel(MainWindow.columnIdEvents, this.controller));
        // Es observador
        this.panelReports = new ReportsPanel("Reports: ", false,this.controller);

        topPanel.add(this.panelEventsEditor);
        topPanel.add(this.panelEventsQueue);
        topPanel.add(this.panelReports);
        centralPanel.add(topPanel);
    }

    private void createBottomPanel(JPanel centralPanel){
        JPanel bottomPanel = new JPanel(), tablesPanel = new JPanel(), graphicPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
        tablesPanel.setLayout(new GridLayout(3, 1));
        graphicPanel.setLayout(new GridLayout(1,1));
        // Es observador
        this.panelVehicles = new TablePanel<Vehicle>("Vehicles", new VehiclesTableModel(MainWindow.columnIdVehicle, this.controller));
        // Es observador
        this.panelRoads = new TablePanel<Road>("Roads", new RoadsTableModel(MainWindow.columnIdRoad, this.controller));
        // Es observador
        this.panelJunctions = new TablePanel<GenericJunction<?>>("Junctions", new JunctionsTableModel(MainWindow.columnIdJunction, this.controller));
        tablesPanel.add(this.panelVehicles);
        tablesPanel.add(this.panelRoads);
        tablesPanel.add(this.panelJunctions);
        // Mapa para los graficos ( dibujo )
        this.mapComponent = new MapComponent(this.controller);
        // Este mapComponent se inserta en el panel inferior
		// Es observador
        graphicPanel.add(new JScrollPane(this.mapComponent, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
        graphicPanel.setBorder(BorderFactory.createTitledBorder("Map"));
        bottomPanel.add(tablesPanel);
        bottomPanel.add(graphicPanel);
        centralPanel.add(bottomPanel);
    }
    private JPanel createMainPanel(){
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		return mainPanel;
    }


    @Override
    public void simulatorError(int time, RoadMap map, List<event.Event> event, SimulationError e) {
    	SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				MainWindow.this.showErrorDialog(e.getMessage());
				MainWindow.this.resetAll();
			}
		});
    }

    @Override
    public void advance(int time, RoadMap map, List<event.Event> event) {
		// Advance no observadores
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				MainWindow.this.panelStatusBar.setMessage("Advance executed with time " + time);
			}
		});
<<<<<<< HEAD
=======

>>>>>>> 4521dedd9b96a75715cb3d1a49e69b16d7a72290
    }

    @Override
    public void addEvent(int time, RoadMap map, List<event.Event> event) {
		// AddEvent no observadores
    }

    @Override
    public void reset(int time, RoadMap map, List<event.Event> event) {
    	// Reset no observadores
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				MainWindow.this.actualFile = null;
				MainWindow.this.panelReports.clear();
			}
		});
    }

    @Override
    public void removeEvent(int time, RoadMap map, List<Event> events) {
        // empty
    }

	public void showErrorDialog(String str){
        JOptionPane.showMessageDialog(this,str);
    }
    public void showDialog(String str){ JOptionPane.showMessageDialog(this, str);}

    public boolean loadFile(){
    	int returnValue = this.fileChooser.showOpenDialog(null);
    	if(returnValue == JFileChooser.APPROVE_OPTION){
    		File file = this.fileChooser.getSelectedFile();
    		try{
    			String str = readFile(file);
    			this.actualFile = file;
    			this.panelEventsEditor.setText(str);
    			this.panelEventsEditor.setBorder(this.actualFile.getName());
    			this.panelStatusBar.setMessage("File " + file.getName() + " of events loaded into the editor");
			}
			catch (FileNotFoundException e){
    			this.showErrorDialog("ERROR: file read did not work " + e.getMessage());
    			return false;
			}
			return true;
		}
		else
			return false;
	}

    private String readFile(File file) throws FileNotFoundException {
		return new Scanner(file).useDelimiter("\\A").next();
	}
	public boolean saveEventsEditor(){
		int returnVal = fileChooser.showSaveDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = fileChooser.getSelectedFile();
			writeFile(file, this.panelEventsEditor.getText());
			return true;
		}
		else
			return false;
    }
	private static void writeFile(File file, String content) {
		try {
			PrintWriter pw = new PrintWriter(file);
			pw.print(content);
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public boolean saveReports(){
		int returnVal = fileChooser.showSaveDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = fileChooser.getSelectedFile();
			writeFile(file, this.panelReports.getText());
			return true;
		}
		else
			return false;
	}
	public void execute(){
		if( this.isPossibleToExecute()){
			// Deshabilitado de las funcionalidades no permitidas durante la ejecucion
			if(control_execute_thread == null) {
				this.setEnabledForExecute(false);
				control_execute_thread = new Thread(new Runnable() {
					@Override
					public void run() {
						int i = 0;
						while (i < MainWindow.this.getSteps() && !Thread.interrupted()) {
							try {
								controller.execute(1);
							} catch (SimulationError simulationError) {
								MainWindow.this.showErrorDialog(simulationError.getMessage());
							}
							try {
								Thread.sleep(MainWindow.this.getDelay());
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
							}
							i++;
						}
						// Habilitado de las funcionalidades no permitidas durante la ejecucion
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								MainWindow.this.setEnabledForExecute(true);
							}
						});
						control_execute_thread = null;
					}
				});
				control_execute_thread.start();
			}
		}
	}

	public void stopExecution(){
		if(this.control_execute_thread != null && this.control_execute_thread.isAlive()){
			this.control_execute_thread.interrupt();
		}
	}

	public void resetAll(){
		this.actualFile = null;
		this.controller.reset();
		this.panelReports.clear();
		this.panelStatusBar.setMessage("Reset done!");
		this.showDialog("Reset done!");
		this.loadFile();
	}

	public void exit(){
		int n = JOptionPane.showOptionDialog(this,
				"Are sure you want to exit?", "Exit",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE, null,
				null, null);
		if (n == 0)
			System.exit(0);
	}

	public void switchOutputStream(OutputOption option) throws SimulationError {
		switch(option){
			case CONSOLE:{
				this.controller.setOutputStream(System.out);
			} break;
			case GRAPHIC:{
				this.controller.setOutputStream(new ReportsPanelStream(this.panelReports.getTextArea()));
			} break;
			default:{
				throw new SimulationError("OutputOption not valid\n");
			}
		}
	}

	public void generateReport(){
        this.panelReports.setText(this.controller.generateReport());
	}
	public void generateSelectedItemsReport(){
		String report = "";
		if(!this.reportsDialog.getSelectedJunctions().isEmpty() || !this.reportsDialog.getSelectedVehicles().isEmpty() || !this.reportsDialog.getSelectedRoads().isEmpty()) {
			for (GenericJunction<?> j : this.reportsDialog.getSelectedJunctions())
				report += j.generateReport(this.toolBar.getTime());
			for (Road r : this.reportsDialog.getSelectedRoads())
				report += r.generateReport(this.toolBar.getTime());
			for (Vehicle v : this.reportsDialog.getSelectedVehicles())
				report += v.generateReport(this.toolBar.getTime());

			this.panelReports.setText(report);
		}
		else
			this.showDialog("Empty Items Selection !");
	}

	public void clearReports(){
        this.panelReports.clear();
    }
	public void clearEventsEditor(){
		this.panelEventsEditor.clear();
	}
	public int getSteps(){
		return this.toolBar.getSteps();
	}
	public int getDelay(){
		return this.toolBar.getDelay();
	}
	public String getEventsEditorText(){
		return this.panelEventsEditor.getText();
	}
	public void setMessage(String str){
		this.showDialog(str);
	}
	public void setVisibleReportsDialog(boolean visible){ this.reportsDialog.setVisible(visible);}
	public void setVisibleBrVehiclesDialog(boolean visible){ this.brVehiclesDialog.setVisible(visible);}
	public void setVisibleRemoveVehiclesDialog(boolean visible) {this.removeVehiclesDialog.setVisible(visible);}

	private void addStatusBar(JPanel mainPanel){
		this.panelStatusBar = new StatusBarPanel("Welcome to the Traffic Simulator !", this.controller);
		mainPanel.add(this.panelStatusBar, BorderLayout.PAGE_END);
	}
	private void addToolBar(JPanel panel){
		this.toolBar = new ToolBar(this, this.controller);
		panel.add(this.toolBar);
	}
	public void insertAtEventsEditor(String str){
		this.panelEventsEditor.insert(str);
	}

	private boolean isPossibleToExecute(){ return !this.panelEventsQueue.isEmpty() || !this.panelVehicles.isEmpty() || !this.panelJunctions.isEmpty() || !this.panelRoads.isEmpty();}

	private void setEnabledForExecute(boolean enabled){
		this.panelEventsEditor.setEnabledForExecute(enabled);
		this.toolBar.setEnabledForExecute(enabled);
		this.menuBar.setEnabledForExecute(enabled);
	public void removeSelectedVehicles(){
		for(Vehicle v : removeVehiclesDialog.getSelectedVehicles()){
			controller.removeVehicle(v.getId());
		}
	}

	public static String[] getColumnIdVehicle() {
		return columnIdVehicle;
	}
}
