/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/


package com.eucalyptus.fault;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class FaultSubsystem {
	// TODO allow customization
	private static File systemFaultDir = new File("/usr/share/eucalyptus/faults"); 
	private static File customFaultDir = new File("/etc/eucalyptus/faults"); 

	public Map<String, Fault> faultMap = new HashMap<String, Fault>();
	private Map<String, String> common = new HashMap<String, String>();
	public static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public void init() {
		// add unknown in case it isn't there
		common.put("unknown", "unknown");
		String locale = System.getenv("LOCALE");
		List<File> faultDirsToCheck = new ArrayList<File>();
		faultDirsToCheck.add(new File(systemFaultDir, "en_US"));
		if (locale != null && !locale.equals("en_US")) {
			faultDirsToCheck.add(new File(systemFaultDir, locale));
			
		}
		faultDirsToCheck.add(new File(customFaultDir, "en_US"));
		if (locale != null && !locale.equals("en_US")) {
			faultDirsToCheck.add(new File(customFaultDir, locale));
			
		}
		for(File faultDirToCheck: faultDirsToCheck) {
			checkFaultDir(faultDirToCheck);
		}
	}

	private void checkFaultDir(File dir) {
		if (dir == null || !dir.isDirectory()) return;
		File[] xmlFilesToCheck = dir.listFiles(new FaultOrCommonXMLFileFilter());
		if (xmlFilesToCheck != null) {
			for (int i=0;i<xmlFilesToCheck.length;i++) {
				checkFaultXMLFile(xmlFilesToCheck[i]);
			}
		}
	}
	private void checkFaultXMLFile(File file) {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(file);
			Element eucaFaultsElement = doc.getDocumentElement();
			eucaFaultsElement.normalize();
			// TODO: incorporate version
			if (!eucaFaultsElement.getNodeName().toLowerCase().equals("eucafaults")) {
				throw new IllegalArgumentException("Root element is not 'eucafaults'");
			} else {
				NodeList commonNodeList = eucaFaultsElement.getElementsByTagName("common");
				if (commonNodeList != null) {
					checkCommonNodeList(commonNodeList);
				}
				NodeList faultsNodeList = eucaFaultsElement.getElementsByTagName("faults");
				if (faultsNodeList != null) {
					checkFaultsNodeList(faultsNodeList);
				}
				NodeList faultNodeList = eucaFaultsElement.getElementsByTagName("fault");
				if (faultNodeList != null) {
					checkFaultNodeList(faultNodeList);
				}
			}
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void checkCommonNodeList(NodeList nodeList) {
		if (nodeList != null) {
			for (int i=0;i<nodeList.getLength();i++) {
				Node node = nodeList.item(i);
				if ((node != null && node instanceof Element)) {
					checkCommonElement((Element) node);
				}
			}
		}
	}

	private void checkCommonElement(Element element) {
		if (element != null) {
			NodeList varNodeList = element.getElementsByTagName("var");
			if (varNodeList != null) {
				for (int i=0;i<varNodeList.getLength();i++) {
					Node varNode = varNodeList.item(i);
					if (varNode != null && (varNode instanceof Element)) {
						String name = getAttributeEvenIfNull((Element) varNode,"name");
						String value = getAttributeEvenIfNull((Element) varNode,"value");
						String localized = getAttributeEvenIfNull((Element) varNode,"localized");
						if (name != null) {
							if (localized != null) {
								common.put(name,  localized);
							} else if (value != null) {
								common.put(name,  value);
							} else {
								// TODO: consider this case
								if (!name.equals("unknown")) {
									common.put(name, "${unknown}");
								}
							}
						}
					}
				}
			}
		}
	}

	private void checkFaultsNodeList(NodeList nodeList) {
		if (nodeList != null) {
			for (int i=0;i<nodeList.getLength();i++) {
				Node node = nodeList.item(i);
				if ((node != null && node instanceof Element)) {
					checkFaultsElement((Element) node);
				}
			}
		}
	}

	private void checkFaultsElement(Element element) {
		if (element != null) {
			NodeList faultNodeList = element.getElementsByTagName("fault");
			if (faultNodeList != null) {
				checkFaultNodeList(faultNodeList);
			}
		}
	}

	private void checkFaultNodeList(NodeList nodeList) {
		if (nodeList != null) {
			for (int i=0;i<nodeList.getLength();i++) {
				Node node = nodeList.item(i);
				if ((node != null && node instanceof Element)) {
					checkFaultElement((Element) node);
				}
			}
		}
	}

	private void checkFaultElement(Element element) {
		if (element != null) {
			String id = element.getAttribute("id");
			if (id == null) {
				throw new IllegalArgumentException("id is null");
			}
			Fault fault = new Fault();
			String faultMessage = getLocalizedMessageAttribute(element);
			String conditionMessage = getSubElementLocalizedMessageAttribute(element, "condition");
			String causeMessage = getSubElementLocalizedMessageAttribute(element, "cause");
			String initiatorMessage = getSubElementLocalizedMessageAttribute(element, "initiator");
			String locationMessage = getSubElementLocalizedMessageAttribute(element, "location");
			String resolutionMessage = getSubElementLocalizedMessageElement(element, "resolution");
			fault = fault.withVar("err_id", "ERR-"+id)
					.withVar("fault_msg", faultMessage)
					.withVar("condition_msg", conditionMessage)
					.withVar("cause_msg", causeMessage)
					.withVar("initiator_msg", initiatorMessage)
					.withVar("location_msg", locationMessage)
					.withVar("resolution_msg", resolutionMessage);
			faultMap.put(id, fault);
		}
	}
	
	private String getSubElementLocalizedMessageElement(Element parentElement,
			String elementName) {
		if (parentElement != null) {
			NodeList nodeList = parentElement.getElementsByTagName(elementName);
			if (nodeList != null && nodeList.getLength() == 1) {
				Node firstElement = nodeList.item(0);
				if (firstElement instanceof Element) {
					return getLocalizedMessageElement((Element) firstElement);
				}
			}
		}	
		return "${unknown}";
	}
	private String getLocalizedMessageAttribute(Element element) {
		if (element != null) {
			String localized = getAttributeEvenIfNull(element, "localized");
			if (localized != null) {
				return localized;
			}
			String message = getAttributeEvenIfNull(element, "message");
			if (message != null) {
				return message;
			}
		}
		return "${unknown}";
	}
	
	private String getAttributeEvenIfNull(Element element, String attributeName) {
		if (element == null) return null;
		NamedNodeMap namedNodeMap = element.getAttributes();
		if (namedNodeMap == null) return null;
		Node localizedNode = namedNodeMap.getNamedItem(attributeName);
		if (localizedNode == null) return null;
		return localizedNode.getNodeValue();
	}

	private String getLocalizedMessageElement(Element element) {
		if (element != null) {
			String localized = getSubElementText(element, "localized");
			String message = getSubElementText(element, "message");
			if (localized != null) {
				return localized;
			}
			if (message != null) {
				return message;
			}
		}
		return "${unknown}";
	}

	private String getSubElementLocalizedMessageAttribute(Element parentElement, String elementName) {
		if (parentElement != null) {
			NodeList nodeList = parentElement.getElementsByTagName(elementName);
			if (nodeList != null && nodeList.getLength() == 1) {
				Node firstElement = nodeList.item(0);
				if (firstElement instanceof Element) {
					// TODO: handle multiplicity
					return getLocalizedMessageAttribute((Element) firstElement);
				}
			}
		}
		return "${unknown}";
	}

	
	
	
    private static String getSubElementText(Element parentElement, String subElementName) {
		NodeList subElementNodeList = parentElement.getElementsByTagName(subElementName);
		if (subElementNodeList == null) return null;
		if (subElementNodeList.getLength() > 1) {
			//throw new IllegalArgumentException("More than one '" + subElementName + "'");
			return null; // TODO deal with multiplicity
		}
		Element subElement = (Element) subElementNodeList.item(0);
		// the text of an element is the first child node
		if (subElement == null) return null;
		NodeList subElementChildrenNodeList = subElement.getChildNodes();
		if (subElementChildrenNodeList == null || subElementChildrenNodeList.getLength() == 0) {
			return null;
		}
		return subElementChildrenNodeList.item(0).getNodeValue();
    }

    public Fault fault(String id) { // todo use enum
    	System.out.println(common);
    	Fault fault = faultMap.get(id);
		if (fault == null) return null;
		for (Map.Entry<String, String> commonEntry: common.entrySet()) {
			fault = fault.withVar(commonEntry.getKey(), commonEntry.getValue());
		}
		// once more just in case elements are "unknown"
		fault.withVar("unknown", common.get("unknown"));
		return fault.withVar("timestamp", formatter.format(new Date()));
	}
    
}


