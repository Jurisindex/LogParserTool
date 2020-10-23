import helpers.LogParseInputData;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;

public class MainController
{
	public GridPane gridPane;

	public final int hBoxBetweenSpaceWidth = 10;
	public final int hBoxMinWidth = 120;
	private static int numberOfRows = 0;
	public final int WIDTH_PER_CHARACTER = 10;

	public HBox guildNameHBox;
	public HBox serverNameHBox;
	public HBox regionHBox;
	public HBox apiKeyHBox;
	public HBox weeksLookbackHBox;
	public HBox inclusionTextHBox;
	public HBox splitIndicatorHBox;
	public HBox spreadsheetIdHBox;
	public Button runButton;
	public HBox statusTextHBox;

	@FXML
	private void initialize()
	{
		//Set up GridPane
		//I just want to do this myself...
		gridPane.setPadding(new Insets(10,10,10,10));
		gridPane.setHgap(5);
		gridPane.setVgap(5);

		//Set up input fields
		//HBox 1: Guild Name
		Label guildLabel = new Label("Guild Name:");
		guildLabel.setMinWidth(hBoxMinWidth);
		TextField guildField = new TextField();
		guildField.setTooltip(new Tooltip("Guild Name, accents and all"));
		guildNameHBox = create2NodeHbox(guildLabel, guildField, hBoxBetweenSpaceWidth);
		add2NodeHBoxToGridPane(gridPane, guildNameHBox, numberOfRows);

		//HBox 2: Server Name
		Label serverLabel = new Label("Server Name:");
		serverLabel.setMinWidth(hBoxMinWidth);
		TextField serverField = new TextField();
		serverField.setTooltip(new Tooltip("Server Name. ex: \"Stalagg\""));
		serverNameHBox = create2NodeHbox(serverLabel, serverField, hBoxBetweenSpaceWidth);
		add2NodeHBoxToGridPane(gridPane, serverNameHBox, numberOfRows);

		//HBox 3: Region
		Label regionLabel = new Label("Region:");
		regionLabel.setMinWidth(hBoxMinWidth);
		TextField regionField = new TextField();
		regionField.setTooltip(new Tooltip("US/EU/CN, etc"));
		regionHBox = create2NodeHbox(regionLabel, regionField, hBoxBetweenSpaceWidth);
		add2NodeHBoxToGridPane(gridPane, regionHBox, numberOfRows);

		//HBox 4: API_KEY (super required)
		Label apiLabel = new Label("API Key:");
		apiLabel.setMinWidth(hBoxMinWidth);
		TextField apiField = new TextField();
		apiField.setTooltip(new Tooltip("No default. Enter your own"));
		apiKeyHBox = create2NodeHbox(apiLabel, apiField, hBoxBetweenSpaceWidth);
		add2NodeHBoxToGridPane(gridPane, apiKeyHBox, numberOfRows);

		//HBox 5: Weeks Lookback (optional, default 8)
		Label weeksLabel = new Label("Weeks Lookback:");
		weeksLabel.setMinWidth(hBoxMinWidth);
		TextField weeksField = new TextField();
		weeksField.setTooltip(new Tooltip("(Optional) Default: 8"));
		weeksLookbackHBox = create2NodeHbox(weeksLabel, weeksField, hBoxBetweenSpaceWidth);
		add2NodeHBoxToGridPane(gridPane, weeksLookbackHBox, numberOfRows);

		//HBox 6: inclusionText (optional, default none)
		Label inclusionLabel = new Label("Inclusion Text:");
		inclusionLabel.setMinWidth(hBoxMinWidth);
		TextField inclusionField = new TextField();
		inclusionField.setTooltip(new Tooltip("In log title. Comma Separated. Ex: Raid2, R2, Raid 2"));
		inclusionTextHBox = create2NodeHbox(inclusionLabel, inclusionField, hBoxBetweenSpaceWidth);
		add2NodeHBoxToGridPane(gridPane, inclusionTextHBox, numberOfRows);

		//HBox 7: splitIndicator (optional, default none)
		Label splitLabel = new Label("Split Indicators:");
		splitLabel.setMinWidth(hBoxMinWidth);
		TextField splitField = new TextField();
		splitField.setTooltip(new Tooltip("For raid-split (Ony/ZG etc) logs: G1,G2,Group1,A-Team,etc"));
		splitIndicatorHBox = create2NodeHbox(splitLabel, splitField, hBoxBetweenSpaceWidth);
		add2NodeHBoxToGridPane(gridPane, splitIndicatorHBox, numberOfRows);

		//HBox 8: spreadsheetID (to be made optional LATER)
		Label spreadsheetLabel = new Label("Spreadsheet ID:");
		spreadsheetLabel.setMinWidth(hBoxMinWidth);
		TextField spreadsheetField = new TextField();
		spreadsheetField.setTooltip(new Tooltip("the ID after the /d/ and before the /edit in your Spreadsheet URL"));
		spreadsheetIdHBox = create2NodeHbox(spreadsheetLabel, spreadsheetField, hBoxBetweenSpaceWidth);
		add2NodeHBoxToGridPane(gridPane, spreadsheetIdHBox, numberOfRows);

		//Set up the Run Button
		runButton = new Button("Run The Program");
		runButton.setOnAction(f -> dryRun(f));
		gridPane.add(runButton, 0, numberOfRows);
		numberOfRows++;

		//Set up process status HBox
		Label staticStatusLabel = new Label("Status:");
		Label dynamicStatusLabel = new Label("Not Yet Started");
		statusTextHBox = create2NodeHbox(staticStatusLabel, dynamicStatusLabel, hBoxBetweenSpaceWidth);
		add2NodeHBoxToGridPane(gridPane, statusTextHBox, numberOfRows);
	}

	private void add2NodeHBoxToGridPane(GridPane gridPane, HBox hBox, int numberOfRows)
	{
		gridPane.add(hBox, 0, numberOfRows);
		this.numberOfRows = numberOfRows + 1;
	}

	public void dryRun(ActionEvent actionEvent)
	{
		Label currentStatus = (Label) statusTextHBox.getChildren().get(1);
		currentStatus.setText("Initialized");

		//Collect all the information.
		LogParseInputData inputData = new LogParseInputData();
		inputData.guildName = findStringInHBoxTextField(guildNameHBox);
		inputData.serverName = findStringInHBoxTextField(serverNameHBox);
		inputData.region = findStringInHBoxTextField(regionHBox);
		inputData.apiKey = findStringInHBoxTextField(apiKeyHBox);
		inputData.weeksLookback = Integer.parseInt(findStringInHBoxTextField(weeksLookbackHBox));
		inputData.inclusionText = Arrays.asList(findStringInHBoxTextField(inclusionTextHBox).split(","));
		inputData.splitIndicators = Arrays.asList(findStringInHBoxTextField(splitIndicatorHBox).split(","));
		inputData.spreadsheetId = findStringInHBoxTextField(spreadsheetIdHBox);

		try
		{
			ApplicationRoot.applicationDryRun(inputData);
		}
		catch (GeneralSecurityException | IOException e)
		{
			e.printStackTrace();
		}
		Integer weeksLookback = Integer.parseInt(weeksLookbackHBox.getChildren().get(1).getAccessibleText());

		currentStatus.setText("Completed");
	}

	private String findStringInHBoxTextField(HBox guildNameHBox)
	{
		for(Node n : guildNameHBox.getChildren())
		{
			if(n.getClass().equals(TextField.class))
			{
				return ((TextField) n).getText();
			}
		}
		return "";
	}

	private HBox create2NodeHbox(Node node1, Node node2, double spacing)
	{
		HBox returnBox = new HBox();
		returnBox.getChildren().addAll(node1, node2);
		returnBox.setSpacing(spacing);

		return returnBox;
	}
}
