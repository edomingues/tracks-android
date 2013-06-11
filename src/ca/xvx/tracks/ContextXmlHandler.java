package ca.xvx.tracks;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class ContextXmlHandler extends DefaultHandler {		
	private int _id;
	private String _name;
	private boolean _hide;
	private int _position;

	private final StringBuffer _text;
	
	private List<Integer> contextIdsOnServer = new LinkedList<Integer>();

	public ContextXmlHandler() {
		super();
		_text = new StringBuffer();
	}
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		if(qName.equals("context")) {
			_id = -1;
			_name = null;
			_hide = false;
			_position = -1;
		} else if(qName.equals("nil-classes")) {
			TodoContext.deleteContextsNotOnServer(Collections.<Integer>emptyList());
		}
		_text.setLength(0);
	}

	@Override
	public void endElement(String uri, String localName, String qName) {		
		if(qName.equals("context")) {			
			TodoContext.checkConflictAndSave(new TodoContext(_id, _name, _position, _hide));
			contextIdsOnServer.add(_id);
		} else if(qName.equals("id")) {
			_id = Integer.valueOf(_text.toString());
		} else if(qName.equals("name")) {
			_name = _text.toString();
		} else if(qName.equals("hide")) {
			_hide = _text.toString().equals("hide") ? true : false;
		} else if(qName.equals("position")) {
			_position = Integer.valueOf(_text.toString());
		} else if(qName.equals("contexts")) {
			TodoContext.deleteContextsNotOnServer(contextIdsOnServer);
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) {
		_text.append(ch, start, length);
	}
}

