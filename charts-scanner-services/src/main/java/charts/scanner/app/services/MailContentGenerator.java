package charts.scanner.app.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hp.gagawa.java.elements.B;
import com.hp.gagawa.java.elements.Div;
import com.hp.gagawa.java.elements.Hr;
import com.hp.gagawa.java.elements.P;
import com.hp.gagawa.java.elements.Table;
import com.hp.gagawa.java.elements.Td;
import com.hp.gagawa.java.elements.Th;
import com.hp.gagawa.java.elements.Tr;

import charts.scanner.app.utils.HelperUtils;

/**
 * Generates HTML based email reports
 * 
 * @author vkommaraju
 *
 */
@Service
public class MailContentGenerator {
	
	@Autowired
	HelperUtils utils;
	
	public String generate(String reportTitle, String reportLabel, List<List<String>> rowData) {
		return generate(reportTitle, reportLabel, getDefaultColumnHeaders(), rowData);
	}

	public String generate(String reportTitle, String reportLabel, List<String> columnHeaders, List<List<String>> rowData) {
		Div div = new Div();
		div.appendChild(getSeparator());
		div.appendChild(getReportHeader(reportTitle, reportLabel));
		div.appendChild(getTable(columnHeaders, rowData));
		div.appendChild(getSeparator());
		return div.write();
	}
	
	 protected Hr getSeparator() {
 		Hr hr = new Hr();
 		return hr;
	 }
	 
	 protected P getReportHeader(String header, String body) {
 		P p = new P();
 		B b = new B();
 		b.appendText(header+": ");
 		p.appendChild(b);
 		p.appendText(body);
 		return p;
 }
	
	protected Table getTable(List<String> columnHeaders, List<List<String>> rowData) {
		Table table = newTable();
		Tr headerRow = new Tr();
		List<Th> headerNodes = getTableHeader(columnHeaders);
		headerNodes.stream().forEach(header -> headerRow.appendChild(header));
		table.appendChild(headerRow);
		List<Tr> rows = getRowData(rowData);
		rows.stream().forEach(row -> table.appendChild(row));
		return table;
	}

	private List<Tr> getRowData(List<List<String>> allRows) {
		List<Tr> rows = Lists.newArrayList();
		for(int i=0; i<allRows.size(); i++) {
			List<String> row = allRows.get(i);
			Tr tr = new Tr();
			tr.setBgcolor(i%2==0 ? "#ffffff" : "#e1e6e8");
			for(int j=0; j<row.size(); j++) {
				Td cell = new Td();
				cell.appendText(row.get(j));
				tr.appendChild(cell);
			}
			rows.add(tr);
		}
		return rows;
	}

	protected Table newTable() {
		Table table = new Table();
		table.setBorder("1");	
		return table;
	}
	
	protected List<Th> getTableHeader(List<String> columnHeaders) {
		List<Th> headerNodes = Lists.newArrayList();
		for(String header : columnHeaders) {
			Th th = new Th();
			th.setBgcolor("#b2c9e8");
			th.appendText(header);
			headerNodes.add(th);
		}
		return headerNodes;
	}
	
	public List<String> getDefaultColumnHeaders() {
		return ImmutableList.of("Ticker", "Scanned Date", "Scan Price", "Current Price", "Yield", "Strategy History", "Chart");
	}

}
