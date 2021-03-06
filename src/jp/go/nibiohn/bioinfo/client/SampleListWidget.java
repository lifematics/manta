package jp.go.nibiohn.bioinfo.client;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jp.go.nibiohn.bioinfo.client.generic.DisableableCheckboxCell;
import jp.go.nibiohn.bioinfo.client.generic.ModifiedSimplePager;
import jp.go.nibiohn.bioinfo.shared.GutFloraConstant;
import jp.go.nibiohn.bioinfo.shared.GutFloraLanguagePack;
import jp.go.nibiohn.bioinfo.shared.SampleEntry;

import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.DefaultSelectionEventManager;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.gwt.view.client.SetSelectionModel;

public class SampleListWidget extends BaseWidget {

	private static final String NO_SELECTED_SAMPLE = "No sample is selected.";

	private static final int PAGE_SIZE = 20;

	private SetSelectionModel<SampleEntry> selectionModel;

	private int selectableNumber = 0;
	
	private Set<SampleEntry> selectedSamples;

	private CellTable<SampleEntry> cellTable;
	
	private List<SampleEntry> allSamples;

	public SampleListWidget(final List<SampleEntry> result, String lang) {
		super("Select samples", lang + GutFloraConstant.NAVI_LINK_SAMPLE);
		this.currentLang = lang;
		this.allSamples = result;
		
		Map<String, String> displayMap = GutFloraLanguagePack.DISPLAY_MAP.get(currentLang);

		int i = 0;
		for (SampleEntry se : result) {
			if (se.hasReads()) {
				i++;
			}
		}
		this.selectableNumber = i;
		
		HorizontalPanel hp = new HorizontalPanel();
		hp.setSpacing(6);

		Widget sampleTable = createTableContent(result);
		hp.add(sampleTable);
		
		hp.add(new HTML("&nbsp;&nbsp;"));
		
		VerticalPanel sampleSelectPanel = new VerticalPanel();
		sampleSelectPanel.setSpacing(6);
		sampleSelectPanel.addStyleName("sampleSelectionContainer");
		hp.add(sampleSelectPanel);
		
		Label header = new Label("Sample selection");
		header.setStyleName("sampleSelectionHeader");
		sampleSelectPanel.add(header);
		
		String labelString1 = displayMap.get("select all");
		if (labelString1 == null) {
			labelString1 = "Select all";
		}
		Label label1 = new Label(labelString1);
		label1.setStyleName("buttonLabel");
		label1.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				for (SampleEntry se : result) {
					if (se.hasReads()) {
						selectionModel.setSelected(se, true);
						// TODO
						infoMessage(messageForSelectedSamples(selectionModel.getSelectedSet().size()));
					}
				}
			}
		});
		sampleSelectPanel.add(label1);
		
		String labelString2 = displayMap.get("select none");
		if (labelString2 == null) {
			labelString2 = "Select none";
		}
		Label label2 = new Label(labelString2);
		label2.setStyleName("buttonLabel");
		label2.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				selectionModel.clear();
				infoMessage(NO_SELECTED_SAMPLE);
			}
		});
		sampleSelectPanel.add(label2);
		
		String labelString3 = displayMap.get("select current page");
		if (labelString3 == null) {
			labelString3 = "Select current page";
		}
		Label label3 = new Label(labelString3);
		label3.setStyleName("buttonLabel");
		label3.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				for (SampleEntry se : cellTable.getVisibleItems()) {
					if (se.hasReads()) {
						selectionModel.setSelected(se, true);
						infoMessage(messageForSelectedSamples(selectionModel.getSelectedSet().size()));
					}
				}
			}
		});
		sampleSelectPanel.add(label3);

		String labelString4 = displayMap.get("unselect current page");
		if (labelString4 == null) {
			labelString4 = "Unselect current page";
		}
		Label label4 = new Label(labelString4);
		label4.setStyleName("buttonLabel");
		label4.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				for (SampleEntry se : cellTable.getVisibleItems()) {
					if (se.hasReads()) {
						selectionModel.setSelected(se, false);
						infoMessage(messageForSelectedSamples(selectionModel.getSelectedSet().size()));
					}
				}
			}
		});
		sampleSelectPanel.add(label4);
		
		sampleSelectPanel.add(new HTML("<hr/>"));

		Label label5 = new Label("Identifier list");
		label5.setStyleName("buttonLabel");
		label5.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				if (idListDialogBox == null) {
					idListDialogBox = createIdListDialogBox();
				}
				
				idListDialogBox.center();

			}
		});
		sampleSelectPanel.add(label5);

		initWidget(hp);
	}

	private Widget createTableContent(final List<SampleEntry> result) {
		VerticalPanel vp = new VerticalPanel();
		cellTable = new CellTable<SampleEntry>();
		cellTable.addStyleName("clickableTable");
		cellTable.setWidth("800px");

		cellTable.setSelectionModel(new NoSelectionModel<SampleEntry>());
		// TODO should I allow keyboard selection?
		cellTable.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);

		final ListDataProvider<SampleEntry> dataProvider = new ListDataProvider<SampleEntry>(result);

		ListHandler<SampleEntry> sortHandler = new ListHandler<SampleEntry>(
				dataProvider.getList()) {
			@Override
			public void onColumnSort(ColumnSortEvent event) {
				super.onColumnSort(event);
				cellTable.setPageStart(0);
			}
		};
		cellTable.addColumnSortHandler(sortHandler);

		selectionModel = new MultiSelectionModel<SampleEntry>();

		cellTable.setSelectionModel(selectionModel, DefaultSelectionEventManager.<SampleEntry> createCheckboxManager());

		Column<SampleEntry, Integer> checkColumn = new Column<SampleEntry, Integer>(new DisableableCheckboxCell(true,
				false)) {
			@Override
			public Integer getValue(SampleEntry object) {
				// Get the value from the selection model.
				if (object.hasReads()) {
					return selectionModel.isSelected(object) ? 1 : -1;
				}
				return 0;
			}
		};
		
		Header<Boolean> checkBoxheader = new Header<Boolean>(new CheckboxCell(true, false)) {
			@Override
			public Boolean getValue() {
				return selectionModel.getSelectedSet().size() == selectableNumber;
			}
		};
		checkBoxheader.setUpdater(new ValueUpdater<Boolean>() {
			@Override
			public void update(Boolean value) {
				if (value) {
					for (SampleEntry se : result) {
						if (se.hasReads()) {
							selectionModel.setSelected(se, true);
							infoMessage(messageForSelectedSamples(selectionModel.getSelectedSet().size()));
						}
					}
				} else {
					selectionModel.clear();
					infoMessage(NO_SELECTED_SAMPLE);
				}
			}
		});
		cellTable.addColumn(checkColumn, checkBoxheader);
		cellTable.setColumnWidth(checkColumn, 40, Unit.PX);

		TextColumn<SampleEntry> colSampleId = new TextColumn<SampleEntry>() {

			@Override
			public String getValue(SampleEntry object) {
				return object.getSampleId();
			}

			@Override
			public String getCellStyleNames(Context context, SampleEntry object) {
				if (object.hasReads()) {
					return super.getCellStyleNames(context, object);
				} else {
					return "disabledSample";
				}
			}
		};
		cellTable.addColumn(colSampleId, "Sample ID");
		colSampleId.setSortable(true);
		sortHandler.setComparator(colSampleId, new Comparator<SampleEntry>() {

			@Override
			public int compare(SampleEntry o1, SampleEntry o2) {
				return o1.getSampleId().compareTo(o2.getSampleId());
			}
		});

		TextColumn<SampleEntry> colAge = new TextColumn<SampleEntry>() {

			@Override
			public String getValue(SampleEntry object) {
				return object.getAge().toString();
			}
		};
		if (currentLang.equals(GutFloraConstant.LANG_JP)) {
			cellTable.addColumn(colAge, "年齢");
		} else {
			cellTable.addColumn(colAge, "Age");
		}
		colAge.setSortable(true);
		sortHandler.setComparator(colAge, new Comparator<SampleEntry>() {

			@Override
			public int compare(SampleEntry o1, SampleEntry o2) {
				return o1.getAge().compareTo(o2.getAge());
			}
		});

		TextColumn<SampleEntry> colGender = new TextColumn<SampleEntry>() {

			@Override
			public String getValue(SampleEntry object) {
				return object.getGender();
			}
		};
		if (currentLang.equals(GutFloraConstant.LANG_JP)) {
			cellTable.addColumn(colGender, "性別");
		} else {
			cellTable.addColumn(colGender, "Sex");
		}
		colGender.setSortable(true);
		sortHandler.setComparator(colGender, new Comparator<SampleEntry>() {

			@Override
			public int compare(SampleEntry o1, SampleEntry o2) {
				return o1.getGender().compareTo(o2.getGender());
			}
		});
		
		TextColumn<SampleEntry> colExpDate = new TextColumn<SampleEntry>() {

			@Override
			public String getValue(SampleEntry object) {
				return object.getExpDate().toString();
			}
		};
		if (currentLang.equals(GutFloraConstant.LANG_JP)) {
			cellTable.addColumn(colExpDate, "測定日");
		} else {
			cellTable.addColumn(colExpDate, "Measurement date");
		}
		colExpDate.setSortable(true);
		sortHandler.setComparator(colExpDate, new Comparator<SampleEntry>() {

			@Override
			public int compare(SampleEntry o1, SampleEntry o2) {
				return o1.getExpDate().compareTo(o2.getExpDate());
			}
		});

		TextColumn<SampleEntry> colProject = new TextColumn<SampleEntry>() {
			
			@Override
			public String getValue(SampleEntry object) {
				return object.getProject();
			}
		};
		if (currentLang.equals(GutFloraConstant.LANG_JP)) {
			cellTable.addColumn(colProject, "コホート");
		} else {
			cellTable.addColumn(colProject, "Cohort name");
		}
		colProject.setSortable(true);
		sortHandler.setComparator(colProject, new Comparator<SampleEntry>() {
			
			@Override
			public int compare(SampleEntry o1, SampleEntry o2) {
				return o1.getProject().compareTo(o2.getProject());
			}
		});

		cellTable.getColumnSortList().push(colSampleId);

		cellTable.setRowCount(result.size(), true);
		cellTable.setPageSize(PAGE_SIZE);

		dataProvider.addDataDisplay(cellTable);

		vp.add(cellTable);

		SimplePager pager = new ModifiedSimplePager();
		pager.setDisplay(cellTable);
		pager.setPageSize(PAGE_SIZE);
		vp.add(pager);

		cellTable.addCellPreviewHandler(new CellPreviewEvent.Handler<SampleEntry>() {

			@Override
			public void onCellPreview(CellPreviewEvent<SampleEntry> event) {
				boolean isClick = "click".equals(event.getNativeEvent().getType());
				if (isClick) {
					int column = event.getColumn();
					if (column == 0) {
						infoMessage(messageForSelectedSamples(selectionModel.getSelectedSet().size()));
					} else {
						// initialize a dialog box
						DialogBox dialogBox = createSampleInfoDialogBox(event.getValue());
						dialogBox.setGlassEnabled(true);
						dialogBox.setAnimationEnabled(false);
						dialogBox.setAutoHideEnabled(true);
						
						int left = 10;
						if (Window.getClientWidth() > 610) {
							left = (Window.getClientWidth() - 600) / 2;
						}
						dialogBox.setPopupPosition(left, 70);
						dialogBox.show();
					}
				}
			}
		});
		
		Button button = new Button("Start", new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				selectedSamples = selectionModel.getSelectedSet();
				
				clearMessage();
				if (selectedSamples == null || selectedSamples.size() == 0) {
					warnMessage("Please select some samples for analysis.");
					return;
				}
				History.newItem(currentLang + GutFloraConstant.NAVI_LINK_ANALYSIS);
				
			}
		});
		button.setWidth("80px");
		vp.add(button);

		return vp;
	}

	private DialogBox createSampleInfoDialogBox(SampleEntry entry) {
		// Create a dialog box and set the caption text
		final DialogBox dialogBox = new DialogBox();
		dialogBox.ensureDebugId("sampleDialogBox");
		dialogBox.setText("Sample information");

		VerticalPanel dialogContents = new VerticalPanel();
		dialogContents.setSpacing(4);
		dialogBox.setWidget(dialogContents);

		SampleInfoWidget sampleInfoWidget = new SampleInfoWidget(entry.getSampleId(), currentLang);
		dialogContents.add(sampleInfoWidget);

		// Add a close button at the bottom of the dialog
		Button closeButton = new Button("Close", new ClickHandler() {
			public void onClick(ClickEvent event) {
				dialogBox.hide();
			}
		});
		closeButton.setWidth("100px");
		dialogContents.add(closeButton);
		dialogContents.setCellHorizontalAlignment(closeButton, HasHorizontalAlignment.ALIGN_CENTER);

		return dialogBox;
	}

	public Set<SampleEntry> getSelectedSamples() {
		return selectedSamples;
	}
	
	private String messageForSelectedSamples(int size) {
		int flag = GutFloraConstant.SINGULAR;
		if (size > 1) {
			flag = GutFloraConstant.PLURAL;
		}
		return size + " " + GutFloraConstant.TREM_SAMPLE[flag] + " " + GutFloraConstant.TO_BE[flag] + " selected.";
	}
	
	private DialogBox idListDialogBox;
	private SimplePanel sampleSelectPanel = new SimplePanel();
	private TextArea textArea = new TextArea();
	private DialogBox createIdListDialogBox() {
		final DialogBox dialogBox = new DialogBox();
		dialogBox.ensureDebugId("idListDialogBox");
		dialogBox.setText("Select samples using identifiers");

		VerticalPanel dialogContents = new VerticalPanel();
		dialogContents.setSpacing(4);
		dialogContents.setWidth("500px");
		dialogBox.setWidget(dialogContents);
		
		dialogContents.add(new Label("Input/Paste the sample identifiers:"));
		textArea.setWidth("450px");
		textArea.setVisibleLines(10);
		sampleSelectPanel.setWidget(textArea);
		dialogContents.add(sampleSelectPanel);
		
		HorizontalPanel buttonHp = new HorizontalPanel();
		buttonHp.setSpacing(12);
		Button okButton = new Button("OK", new ClickHandler() {
			public void onClick(ClickEvent event) {
				Set<String> idSet = new HashSet<String>(Arrays.asList(processQueryString(textArea.getText())));
				for (SampleEntry se : allSamples) {
					if (se.hasReads() && idSet.contains(se.getSampleId())) {
						selectionModel.setSelected(se, true);
					}
				}
				infoMessage(messageForSelectedSamples(selectionModel.getSelectedSet().size()));
				dialogBox.hide();
			}
		});
		okButton.setWidth("80px");
		buttonHp.add(okButton);

		Button resetButton = new Button("Reset", new ClickHandler() {
			public void onClick(ClickEvent event) {
				textArea.setText("");
			}
		});
		resetButton.setWidth("80px");
		buttonHp.add(resetButton);
		
		// Add a close button at the bottom of the dialog
		Button closeButton = new Button("Close", new ClickHandler() {
			public void onClick(ClickEvent event) {
				dialogBox.hide();
			}
		});
		closeButton.setWidth("80px");
		buttonHp.add(closeButton);
		dialogContents.add(buttonHp);
		dialogContents.setCellHorizontalAlignment(buttonHp, HasHorizontalAlignment.ALIGN_CENTER);

		dialogBox.setGlassEnabled(true);
		dialogBox.setAnimationEnabled(false);
		dialogBox.setAutoHideEnabled(true);

		return dialogBox;
	}
	
	private String[] processQueryString(String queryString) {
		String string = queryString.replaceAll("[,|;]", " ");
		String[] ids = string.split("\\s+");
		return ids;
	}

}
