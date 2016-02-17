/*
 * Tile Cutter by Sebastian Śledź (C) 2011
 * Some rights reserved.
 *
 * Home page: http://www.garaz.net
 *      Mail: sebasledz@gmail.com
 */

package net.garaz.tileCutter;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.util.ArrayList;
import java.util.prefs.Preferences;

import javax.imageio.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

public class MainWindow extends JFrame
{
	private static final long serialVersionUID = 1L;
	
	private ArrayList<Component> components = null;
	
	private JFileChooser destinationPathDialog = null;
	private JFileChooser imagesDialog = null;
	
	private DefaultListModel imagesListModel = null;
	private JList imagesList = null;
	
	private boolean directionLeftToRight = true;
	private boolean directionTopToBottom = true;
	private boolean rowOrdered = true;
	private boolean createSeparatedDirForEachImage = false;
	
	private int sliceWidth = 256;
	private int sliceHeight = 256;
	private int widthOverlay = 16;
	private int heightOverlay = 16;
	
	private JButton cancelSlicing = null;
	private JTextArea console = null;
	private JTextField destinationPathTextFiled = null;
	private JTextField sliceWidthTextField = null;
	private JTextField sliceHeightTextField = null;
	private JTextField sliceWidthOverlayTextField = null;
	private JTextField sliceHeightOverlayTextField = null;
	
	private Thread thread = null;
	
	
	public MainWindow()
	{
		super("Tile Cutter");
		
		// Load last configuration and prepare UI elements
		Preferences preferences = Preferences.userRoot();
		directionLeftToRight = preferences.getBoolean("directionLeftToRight", directionLeftToRight);
		directionTopToBottom = preferences.getBoolean("directionTopToBottom", directionTopToBottom);
		rowOrdered = preferences.getBoolean("rowOrdered", rowOrdered);
		createSeparatedDirForEachImage = preferences.getBoolean("createSeparatedDirForEachImage", createSeparatedDirForEachImage);
		sliceWidth = preferences.getInt("sliceWidth", sliceWidth);
		sliceHeight = preferences.getInt("sliceHeight", sliceHeight);
		widthOverlay = preferences.getInt("widthOverlay", widthOverlay);
		heightOverlay = preferences.getInt("heightOverlay", heightOverlay);
		
		destinationPathDialog = new JFileChooser();
		destinationPathDialog.setDialogTitle("Select destination path");
		destinationPathDialog.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		destinationPathDialog.setMultiSelectionEnabled(false);
		destinationPathDialog.setAcceptAllFileFilterUsed(false);
		
		imagesDialog = new JFileChooser();
		imagesDialog.setDialogTitle("Select images");
		imagesDialog.setFileFilter(new FileNameExtensionFilter("Images (GIF, JPEG, PNG)", "gif", "jpg", "jpeg", "png"));
		imagesDialog.setFileSelectionMode(JFileChooser.FILES_ONLY);
		imagesDialog.setMultiSelectionEnabled(true);
		
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		preparePositionAndSize();
		prepareContent();
		
		destinationPathTextFiled.setText(preferences.get("destinationPath", "."));
		destinationPathDialog.setCurrentDirectory(new File(destinationPathTextFiled.getText()));
		imagesDialog.setCurrentDirectory(new File(preferences.get("imagesCurrentDir", ".")));
		
		setVisible(true);
		
		// Set hook for handling application close
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				// Save settings on application exit
				Preferences preferences = Preferences.userRoot();
				
				preferences.putBoolean("directionLeftToRight", directionLeftToRight);
				preferences.putBoolean("directionTopToBottom", directionTopToBottom);
				preferences.putBoolean("rowOrdered", rowOrdered);
				preferences.putBoolean("createSeparatedDirForEachImage", createSeparatedDirForEachImage);
				
				preferences.put("sliceWidth", sliceWidthTextField.getText());
				preferences.put("sliceHeight", sliceHeightTextField.getText());
				preferences.put("widthOverlay", sliceWidthOverlayTextField.getText());
				preferences.put("heightOverlay", sliceHeightOverlayTextField.getText());

				preferences.put("destinationPath", destinationPathTextFiled.getText());
				preferences.put("imagesCurrentDir", imagesDialog.getCurrentDirectory().getPath());
				
				try {
					preferences.sync();
				} catch (Exception ex) {}
			}
		}));
	}
	
	private void prepareContent()
	{
		components = new ArrayList<Component>();
		
		// Horizontal slicing direction
		ButtonGroup horizontalSlicingGroup = new ButtonGroup();
		JPanel slicingDirectionH = new JPanel();		
		slicingDirectionH.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Horizontal slicking direction"));
		
		JRadioButton leftToRight = new JRadioButton("From left to right");
		leftToRight.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { directionLeftToRight = true; } });
		horizontalSlicingGroup.add(leftToRight);
		slicingDirectionH.add(leftToRight);
		
		JRadioButton rightToLeft = new JRadioButton("From right to left");
		rightToLeft.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { directionLeftToRight = false; } });
		horizontalSlicingGroup.add(rightToLeft);
		slicingDirectionH.add(rightToLeft);
		
		leftToRight.setSelected(directionLeftToRight);
		rightToLeft.setSelected( ! directionLeftToRight);
		
		components.add(leftToRight);
		components.add(rightToLeft);
		
		
		// Vertical slicing direction
		ButtonGroup verticalSlicingGroup = new ButtonGroup();
		JPanel slicingDirectionV = new JPanel();		
		slicingDirectionV.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Vertical slicking direction"));
		
		JRadioButton topToBottom = new JRadioButton("From top to bottom");
		topToBottom.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { directionTopToBottom = true; } });
		verticalSlicingGroup.add(topToBottom);
		slicingDirectionV.add(topToBottom);
		
		JRadioButton bottomToTop = new JRadioButton("From bottom to top");
		bottomToTop.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { directionTopToBottom = false; } });
		verticalSlicingGroup.add(bottomToTop);
		slicingDirectionV.add(bottomToTop);
		
		topToBottom.setSelected(directionTopToBottom);
		bottomToTop.setSelected( ! directionTopToBottom);
		
		components.add(topToBottom);
		components.add(bottomToTop);
		
		
		// Row/column order selection
		ButtonGroup orderSlicingGroup = new ButtonGroup();
		JPanel orderGroup = new JPanel();
		orderGroup.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Slicing order"));
		
		JRadioButton rowOrder = new JRadioButton("Row order");
		rowOrder.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { rowOrdered = true; } });
		orderSlicingGroup.add(rowOrder);
		orderGroup.add(rowOrder);
		
		JRadioButton colOrder = new JRadioButton("Column order");
		colOrder.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { rowOrdered = false; } });
		orderSlicingGroup.add(colOrder);
		orderGroup.add(colOrder);
		
		rowOrder.setSelected(rowOrdered);
		colOrder.setSelected( ! rowOrdered);
		
		components.add(rowOrder);
		components.add(colOrder);
		
		
		// Slice size
		JPanel sliceSizePanel = new JPanel();
		sliceSizePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Slice size"));
		
		sliceSizePanel.add(new JLabel("Width"));
		
		sliceWidthTextField = new JTextField(Integer.toString(sliceWidth));
		sliceWidthTextField.setMinimumSize(new Dimension(200, sliceWidthTextField.getMinimumSize().height));
		sliceWidthTextField.setPreferredSize(sliceWidthTextField.getMinimumSize());
		sliceSizePanel.add(sliceWidthTextField);
		
		sliceSizePanel.add(new JLabel("Height"));
		
		sliceHeightTextField = new JTextField(Integer.toString(sliceHeight));
		sliceHeightTextField.setMinimumSize(new Dimension(200, sliceHeightTextField.getMinimumSize().height));
		sliceHeightTextField.setPreferredSize(sliceHeightTextField.getMinimumSize());
		sliceSizePanel.add(sliceHeightTextField);
		
		components.add(sliceWidthTextField);
		components.add(sliceHeightTextField);

		sliceSizePanel.add(new JLabel("Overlay width"));

		sliceWidthOverlayTextField = new JTextField(Integer.toString(widthOverlay));
		sliceWidthOverlayTextField.setMinimumSize(new Dimension(200, sliceWidthOverlayTextField.getMinimumSize().height));
		sliceWidthOverlayTextField.setPreferredSize(sliceWidthOverlayTextField.getMinimumSize());
		sliceSizePanel.add(sliceWidthOverlayTextField);
		
		sliceSizePanel.add(new JLabel("Overlay height"));
		
		sliceHeightOverlayTextField = new JTextField(Integer.toString(heightOverlay));
		sliceHeightOverlayTextField.setMinimumSize(new Dimension(200, sliceHeightOverlayTextField.getMinimumSize().height));
		sliceHeightOverlayTextField.setPreferredSize(sliceHeightOverlayTextField.getMinimumSize());
		sliceSizePanel.add(sliceHeightOverlayTextField);
		
		components.add(sliceWidthOverlayTextField);
		components.add(sliceHeightOverlayTextField);
		
		
		// Destination path
		JPanel sliceDestinationPanel = new JPanel();
		sliceDestinationPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Destination path"));
		
		sliceDestinationPanel.add(new JLabel("Path"));
		
		destinationPathTextFiled = new JTextField(".");
		destinationPathTextFiled.setEditable(false);
		destinationPathTextFiled.setMinimumSize(new Dimension(400, destinationPathTextFiled.getMinimumSize().height));
		destinationPathTextFiled.setPreferredSize(destinationPathTextFiled.getMinimumSize());
		sliceDestinationPanel.add(destinationPathTextFiled);
		
		JButton selectDestionantionPath = new JButton("Select");
		selectDestionantionPath.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				selectDesctinationPath();
			}
		});
		sliceDestinationPanel.add(selectDestionantionPath);
		
		JCheckBox createDir = new JCheckBox("Create sub directories for each image");
		createDir.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				createSeparatedDirForEachImage = ((JCheckBox)e.getSource()).isSelected();
			}
		});
		createDir.setSelected(createSeparatedDirForEachImage);
		sliceDestinationPanel.add(createDir);
		
		components.add(selectDestionantionPath);
		components.add(createDir);
		
		
		// Buttons
		JPanel buttons = new JPanel();
		
		JButton addImages = new JButton("Add images");
		addImages.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addImages();
			}
		});
		
		JButton remImages = new JButton("Remove selected");
		remImages.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				removeImages();
			}
		});
		remImages.setForeground(Color.red);
		
		JButton sliceImages = new JButton("Slice images");
		sliceImages.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				sliceImages();
			}
		});
		
		cancelSlicing = new JButton("Cancel slicing");
		cancelSlicing.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cancelSlicing();
			}
		});
		cancelSlicing.setEnabled(false);
		
		buttons.add(addImages);
		buttons.add(sliceImages);
		buttons.add(cancelSlicing);
		buttons.add(remImages);
		
		components.add(addImages);
		components.add(remImages);
		components.add(sliceImages);
		
		
		// Images list
		imagesListModel = new DefaultListModel();
		
		imagesList = new JList(imagesListModel);
		imagesList.setLayoutOrientation(JList.VERTICAL);
		imagesList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		
		JScrollPane scrolledImagesList = new JScrollPane(imagesList);
		scrolledImagesList.setBorder(BorderFactory.createLoweredBevelBorder());
		
		components.add(imagesList);
		
		
		// Console
		console = new JTextArea();
		console.setEditable(false);
		
		JScrollPane consoleScroll = new JScrollPane(console);
		consoleScroll.setBorder(BorderFactory.createLoweredBevelBorder());
		
		
		
		setLayout(new GridBagLayout());
		
		JPanel options = new JPanel();
		options.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), " Settings "));
		options.setLayout(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(2, 0, 2, 0);
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		options.add(slicingDirectionH, c);
		c.gridx = 1;
		options.add(slicingDirectionV, c);
		c.gridx = 2;
		options.add(orderGroup, c);
		c.gridx = 3;
		options.add(sliceSizePanel, c);
		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 4;
		options.add(sliceDestinationPanel, c);
		
		c.insets = new Insets(5, 5, 5, 5);
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.weighty = 0;
		add(options, c);
		
		c.gridx = 0;
		c.gridy = 1;
		add(buttons, c);
		
		c.weighty = 2;
		c.gridy = 3;
		add(scrolledImagesList, c);
		c.gridy = 5;
		add(consoleScroll, c);
		
		c.insets = new Insets(5, 5, 0, 5);
		c.weighty = 0;
		c.gridy = 2;
		add(new JLabel("Images list"), c);
		c.gridy = 4;
		add(new JLabel("Console"), c);
	}

	private void preparePositionAndSize()
	{
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Dimension screenSize = toolkit.getScreenSize();
		Dimension windowSize = new Dimension((int)(screenSize.width * 0.8), (int)(screenSize.height * 0.8));
		
		if (screenSize.width - windowSize.width < 100) windowSize.width = screenSize.width - 100;
		if (screenSize.height - windowSize.height < 100) windowSize.height = screenSize.height - 100;
		
		Point windowPosition = new Point((screenSize.width - windowSize.width) / 2,
				(screenSize.height - windowSize.height) / 2);
		
		setLocation(windowPosition);
		setSize(windowSize);
	}
	
	private void selectDesctinationPath()
	{
		if (destinationPathDialog.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
		{
			destinationPathTextFiled.setText(destinationPathDialog.getSelectedFile().getPath());
		}
	}
	
	private void addImages()
	{
		if (imagesDialog.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
		{
			for (File file : imagesDialog.getSelectedFiles())
			{
				imagesListModel.addElement(file.getPath());
			}
		}
	}
	
	private void removeImages()
	{
		int [] indices = imagesList.getSelectedIndices();
		
		for (int i = indices.length - 1; i >= 0; --i)
		{
			imagesListModel.remove(indices[i]);
		}
	}
	
	private void lockUI()
	{
		for (Component c : components)
		{
			c.setEnabled(false);
		}
		cancelSlicing.setEnabled(true);
		
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	}
	
	private void unlockUI()
	{
		for (Component c : components)
		{
			c.setEnabled(true);
		}
		cancelSlicing.setEnabled(false);
		
		setCursor(Cursor.getDefaultCursor());
	}
	
	private void log(String s)
	{
		synchronized (console)
		{
			console.append(s);
			console.append("\n");
			console.setCaretPosition(console.getText().length());
		}
	}
	
	private void saveSlice(BufferedImage image, int x, int y, String slicePath, int sliceNumber) throws IOException
	{
		final int width = image.getWidth();
		final int height = image.getHeight();
		
		int xx = x + sliceWidth;
		int yy = y + sliceHeight;
		
		if (x < 0) x = 0;
		else if (x >= width) x = width - 1;
		if (y < 0) y = 0;
		else if (y >= height) y = height - 1;
		
		if (xx < 0) xx = 0;
		else if (xx >= width) xx = width - 1;
		if (yy < 0) yy = 0;
		else if (yy >= height) yy = height - 1;
		
		final int currentSliceWidth = xx - x;
		final int currentSliceHeight = yy - y;
		
		BufferedImage slice = new BufferedImage(currentSliceWidth, currentSliceHeight, BufferedImage.TYPE_INT_ARGB_PRE);
		Graphics2D sliceG2D = slice.createGraphics();
		
		sliceG2D.drawImage(image,
				0, 0, currentSliceWidth, currentSliceHeight,
				x, y, xx, yy,
				null);
		
		ImageIO.write(slice, "png", new File(slicePath + Integer.toString(sliceNumber) + ".png"));
	}
	
	private int sliceImage(String path)
	{
		if (threadSchouldStop()) return 0;
		
		String logString = "Slicing \"" + path + "\" ";
		
		int lastPathSeparator = path.lastIndexOf('/') > path.lastIndexOf('\\') ? path.lastIndexOf('/') : path.lastIndexOf('\\');
		int lastFileTypeSeparator = path.lastIndexOf('.') > lastPathSeparator ? path.lastIndexOf('.') : path.length();
		String fileName = path.substring(lastPathSeparator, lastFileTypeSeparator);
		String dirPath = destinationPathTextFiled.getText();
		
		// Create destination directory
		if (createSeparatedDirForEachImage)
		{
			dirPath += "/" + fileName;
			
			File file = new File(dirPath);
			file.mkdirs();
		}
		
		String slicesPath = dirPath + "/" + fileName + "_";
		
		try
		{
			BufferedImage image = ImageIO.read(new File(path));
			
			int width = image.getWidth();
			int height = image.getHeight();
			int slicesH = (int)Math.ceil((double)(width - widthOverlay) / (sliceWidth - widthOverlay) );
			int slicesV = (int)Math.ceil((double)(height - heightOverlay) / (sliceHeight - heightOverlay) );
			
			int beginH, stepH;
			int beginV, stepV;
			
			if (directionLeftToRight)
			{
				beginH = 0;
				stepH = 1;
			}
			else
			{
				beginH = width - sliceWidth;
				stepH = -1;
			}
			
			if (directionTopToBottom)
			{
				beginV = 0;
				stepV = 1;
			}
			else
			{
				beginV = height - sliceHeight;
				stepV = -1;
			}
			
			int sliceNumber = 0;
			if (rowOrdered)
			{
				for (int y = 0; y < slicesV; ++y)
				{
					for (int x = 0; x < slicesH; ++x)
					{
						saveSlice(image, beginH + x * stepH * (sliceWidth - widthOverlay),
								beginV + y * stepV * (sliceHeight - heightOverlay), slicesPath, sliceNumber++);
						
						if (threadSchouldStop()) return 0;
					}
				}
			}
			else
			{
				for (int x = 0; x < slicesH; ++x)
				{
					for (int y = 0; y < slicesV; ++y)
					{
						saveSlice(image, beginH + x * stepH * (sliceWidth - widthOverlay),
								beginV + y * stepV * (sliceHeight - heightOverlay), slicesPath, sliceNumber++);
						
						if (threadSchouldStop()) return 0;
					}
				}
			}
			
			logString += "ok";
			log(logString);
		}
		catch (IOException e)
		{
			logString += "fail (" + e.getMessage() + ")";
			log(logString);
			
			return 1;
		}
		
		return 0;
	}
	
	private void sliceImages()
	{
		sliceWidth = Integer.parseInt(sliceWidthTextField.getText());
		sliceHeight = Integer.parseInt(sliceHeightTextField.getText());
		
		synchronized (this)
		{
			thread = new Thread(new Runnable()
			{
				public void run()
				{
					if (threadSchouldStop()) return;
					
					lockUI();
					
					int errors = 0;
					
					log("\nSlicing images:");
					
					int i = 0;
					for (; i < imagesListModel.size(); ++i)
					{
						errors += sliceImage((String)imagesListModel.get(i));
						
						if (threadSchouldStop())
						{
							log("\nCanceled...");
							break;
						}
					}
					
					log("\nSummary: images=" + Integer.toString(i) + " errors=" + Integer.toString(errors));
					
					unlockUI();
					
					synchronized (this)
					{
						thread = null;
					}
				}
			});
			
			thread.start();
		}
	}
	
	private void cancelSlicing()
	{
		synchronized (this)
		{
			thread = null;
		}
	}
	
	private boolean threadSchouldStop()
	{
		boolean schouldStop = true;
		
		synchronized (this)
		{
			schouldStop = Thread.currentThread() != thread;
		}
		
		return schouldStop;
	}
}
