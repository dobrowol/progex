/*** In The Name of Allah ***/
package ghaffarian.progex.graphs.cfg;

import ghaffarian.graphs.Edge;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ghaffarian.progex.utils.StringUtils;
import ghaffarian.nanologger.Logger;
import ghaffarian.progex.graphs.AbstractProgramGraph;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.Predicate;

/**
 * Control Flow Graph (CFG).
 * 
 * @author Seyed Mohammad Ghaffarian
 */



public class ControlFlowGraph extends AbstractProgramGraph<CFNode, CFEdge> {
	
	private String pkgName;
	public final String fileName;
	private final List<CFNode> methodEntries;
	private Set<Edge<String,String>> stringEdges;
	private Set<String> logNodes;	
	Map<String, CFNode> nodeNamesMap = new LinkedHashMap<>();
	Map<String, String> logTemplatesMap = new HashMap<>();;

	public ControlFlowGraph(String fileName) {
		super();
		this.pkgName = "";
		this.fileName = fileName;
		methodEntries = new ArrayList<>();
		stringEdges = new HashSet<>();
		logNodes = new HashSet<>();
        properties.put("label", "CFG of " + fileName);
        properties.put("type", "Control Flow Graph (CFG)");
	}
	
	public void setPackage(String pkg) {
		pkgName = pkg;
	}
	
	public String getPackage() {
		return pkgName;
	}
	
	public void addMethodEntry(CFNode entry) {
		methodEntries.add(entry);
	}
	
	public CFNode[] getAllMethodEntries() {
		return methodEntries.toArray(new CFNode[methodEntries.size()]);
	}

	public void importDOT(String dotFilePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(dotFilePath))) {
            String line;
            Pattern nodePattern = Pattern.compile("  (v\\d+)  \\[label=\\\"(\\d+):  (.*)\\\"\\];");
			Pattern endNodePattern = Pattern.compile("  (v\\d+)  \\[label=\\\"(.*)\\\"\\];");
            Pattern edgePattern = Pattern.compile("  (v\\d+) -> (v\\d+)(?: \\[label=\"(.*?)\"\\])?;");
			Pattern labelledEdgePattern = Pattern.compile("  (v\\d+) -> (v\\d+)  \\[label=\\\"(.*?)\\\"\\];");
            Pattern logPattern = Pattern.compile("info\\(|debug\\(|error\\(|trace\\(|warn\\(|fatal\\(");
            boolean isParsingNodes = false;
            boolean isParsingEdges = false;
			
            while ((line = reader.readLine()) != null) {
                if (line.trim().contains(" graph-vertices")) {
					// Logger.info("graph-vertices");
                    isParsingNodes = true;
                    isParsingEdges = false;
                } else if (line.trim().contains(" graph-edges")) {
					// Logger.info("graph-edges");
                    isParsingNodes = false;
                    isParsingEdges = true;
                } else if (line.trim().contains(" end-of-graph")) {
					// Logger.info("end-of-graph");
                    break;
                } else {
                    if (isParsingNodes) {
                        Matcher nodeMatcher = nodePattern.matcher(line);
						Matcher endNodeMatcher = endNodePattern.matcher(line);
						
                        if (nodeMatcher.matches()) {
                            String nodeId = nodeMatcher.group(1);
                            Integer nodeLine = Integer.parseInt(nodeMatcher.group(2));
							String nodeCode = nodeMatcher.group(3);
							String[] splits = nodeCode.split("\\.");
							if (splits.length > 1){
								// System.out.println("original line "+nodeCode);
								// System.out.println("split 1 " + splits[1]);
								Matcher logMatcher = logPattern.matcher(splits[1]);
								if (logMatcher.find())
								{
									// System.out.println("logMatcher matches true");
									String template = convertLogPrintStatementToLogTemplate(nodeCode);
									
									// System.out.println("template found "+template);
									if (template.compareTo(".*")!=0 && !template.contains("$")){
										System.out.println("original line "+nodeCode);
										System.out.println("template "+ nodeId + " for line "+template);
										logTemplatesMap.put(nodeId, template);
									}
								}
							}
                            // Create CFNode and add it to CFG
                            CFNode node = new CFNode();
							node.setLineOfCode(nodeLine);
							node.setCode(nodeCode);
							nodeNamesMap.put(nodeId, node);
                            allVertices.add(node);
                        } else if (endNodeMatcher.matches()){
							String nodeId = endNodeMatcher.group(1);
							String nodeCode = endNodeMatcher.group(2);
							String[] splits = nodeCode.split("\\.");
							if (splits.length > 1){
								// System.out.println("split 1 " + splits[1]);
								Matcher logMatcher = logPattern.matcher(splits[1]);
								
								if (logMatcher.find())
								{
									String template = convertLogPrintStatementToLogTemplate(nodeCode);
									// System.out.println("template found "+template);
									if (template != ".*"&& !template.contains("$")&&!template.contains("%")){
										System.out.println("original line "+nodeCode);
										System.out.println("template "+ nodeId + " for line "+template);
										logTemplatesMap.put(nodeId, template);
									}
								}
							}
                            // Create CFNode and add it to CFG
                            CFNode node = new CFNode();
							node.setCode(nodeCode);
							nodeNamesMap.put(nodeId, node);
                            allVertices.add(node);
						}
						else{
							Logger.info(line+" matches: false");
						}
                    } else if (isParsingEdges) {
                        Matcher edgeMatcher = edgePattern.matcher(line);
						Matcher labelledEdgeMatcher = labelledEdgePattern.matcher(line);
						
                        if (edgeMatcher.matches()) {
                            String srcNodeId = edgeMatcher.group(1);
                            String trgNodeId = edgeMatcher.group(2);
							CFEdge edgeLbl = new CFEdge(CFEdge.Type.EPSILON);
							
							CFNode srcNode = nodeNamesMap.get(srcNodeId);
							CFNode trgNode = nodeNamesMap.get(trgNodeId);
                            // Create CFEdge and add it to CFG
                            
							Edge<CFNode, CFEdge> edge = new Edge<CFNode, CFEdge>(srcNode,edgeLbl, trgNode);
							Edge<String, String> str_edge = new Edge<String, String>(srcNodeId, "", trgNodeId);
							allEdges.add(edge);
							stringEdges.add(str_edge);
                        }
						else if (labelledEdgeMatcher.matches()) {
                            String srcNodeId = labelledEdgeMatcher.group(1);
                            String trgNodeId = labelledEdgeMatcher.group(2);
	
							String edgeLabel = labelledEdgeMatcher.group(3);
							CFEdge edgeLbl = new CFEdge(CFEdge.Type.fromString(edgeLabel));

							CFNode srcNode = nodeNamesMap.get(srcNodeId);
							CFNode trgNode = nodeNamesMap.get(trgNodeId);
                            // Create CFEdge and add it to CFG
                            
							Edge<CFNode, CFEdge> edge = new Edge<CFNode, CFEdge>(srcNode,edgeLbl, trgNode);
							Edge<String, String> str_edge = new Edge<String, String>(srcNodeId, edgeLabel, trgNodeId);
							allEdges.add(edge);
							stringEdges.add(str_edge);
                        }else{
							Logger.info(line+" matches: false");
						}
						
                    }
                }
            }
        }
		catch (UnsupportedEncodingException ex) {
			Logger.error(ex);
		}
		Logger.info("importDOT finished. Loaded "+allEdges.size()+" edges and "+allVertices.size()+" vertices and "+logNodes.size()+" logs");
    }
	private String convertLogPrintStatementToLogTemplate(String logCode){
		Pattern logPattern = Pattern.compile("\\'([^\\+]+)\\'");
		String replacementString = logCode.replaceAll("',", "'+");
		replacementString = replacementString.replaceAll(",'|,\\s'", "+'");
		replacementString = replacementString.replaceAll("\\+([^\\+\\']+)\\+", "+REPL+");
		replacementString = replacementString.replaceAll("\\+([^\\+\\']+)$", "+REPL");
		System.out.println("what we match "+replacementString);
		Matcher edgeMatcher = logPattern.matcher(replacementString);
		String template = "";
		Integer lastPosition = 0;
		while(edgeMatcher.find()){
			if (template!= ""){
				template += ".*";
			}
			lastPosition = edgeMatcher.end();
			//String literal = Pattern.quote(edgeMatcher.group(1));
			String literal = edgeMatcher.group(1);
			// literal = literal.replaceAll("\\{\\}", ".*");
			// literal = literal.replaceAll("\\(", "\\\\(");
			// literal = literal.replaceAll("\\)", "\\\\)");
			// literal = literal.replaceAll("\\]", "\\\\]");
			// literal = literal.replaceAll("\\[", "\\\\[");
			// literal = literal.replaceAll("\\{", "\\\\{");
			// literal = literal.replaceAll("\\}", "\\\\}");
			String [] parts = literal.split("\\{\\}");
			
			if (parts.length>1){
				literal = "";
				for (int i =0; i <parts.length-1; i++){
					literal += Pattern.quote(parts[i])+".*"+Pattern.quote(parts[i+1]);
				}
			}else{
				literal = Pattern.quote(literal);
			}
			
			//literal = literal.replaceAll("\\\\", "\\\\\\\\");
			
			template += literal;
		}
		if (lastPosition < replacementString.length()-1){
			template += ".*";
		}
		return template;
	}

	public void createLogControlFlow(String logFilePath) throws FileNotFoundException, IOException{

		Set<LogNode> roots = TreeLogBuilder.buildTree(stringEdges);
		Logger.info("printing log nodes with size "+logNodes.size());
		for (String logNode : logNodes){
			Logger.info(nodeNamesMap.get(logNode).getCode());
		}
		Predicate<LogNode> logPredicate = NodePredicate.isInValueSet(logNodes);
		TreeSelector treeSelector = new TreeSelector();
		treeSelector.selectConnectedNodesWithProperty(roots, logPredicate);
		treeSelector.print_subtrees();
		System.out.println("logTemplatesMap");
		for (Map.Entry<String, String> entry : logTemplatesMap.entrySet()) {
			 System.out.println(entry.getKey() +" " +entry.getValue());
		}
		try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath))) {
            String line;
			Integer i = 0;
			Integer average_number_of_templates_per_line = 0;
			Integer line_count_with_unique_template = 0;
            while ((line = reader.readLine()) != null) {

				i+=1;
				

				Set<String> templateCandidates = new HashSet<>();
				int j =0;
				Pattern templatePattern1 = Pattern.compile("\\Qping from (\\E.*\\Q) to agent's host ip address (\\E.*");
				Matcher matcher1 = templatePattern1.matcher(line);
				if (matcher1.find()){
					//System.out.println("Artificial matcher matches "+line);
				}
				for (Map.Entry<String, String> entry : logTemplatesMap.entrySet()){
					int percent = (j * 100) / logTemplatesMap.size();
					//System.out.print("\rProgress: " + percent + "% [" + new String(new char[percent]).replace("\0", "#") + "]");
					//System.out.println("matching pattern "+entry.getValue()+" to line "+line);
					Pattern templatePattern = Pattern.compile(entry.getValue());
					Matcher matcher = templatePattern.matcher(line);
					if (matcher.find()){
						System.out.println(entry.getKey() + " " +entry.getValue()+" template matches true "+line);
						templateCandidates.add(entry.getKey());
					}
					j++;
				}
				System.out.println(templateCandidates.size()+" templates matches "+line);
				average_number_of_templates_per_line += line_count_with_unique_template;
				if (templateCandidates.size() == 1){
					line_count_with_unique_template += 1;
				}
			}
			System.out.println();
			System.out.println("There are "+line_count_with_unique_template+ " lines with unique template match, out of "+ i);
			System.out.println("Average number of templates per line "+average_number_of_templates_per_line/i);
		}
		catch (FileNotFoundException ex) {
			Logger.error(ex);
		}
	}

    @Override
	public void exportDOT(String outDir) throws FileNotFoundException {
        if (!outDir.endsWith(File.separator))
            outDir += File.separator;
        File outDirFile = new File(outDir);
        outDirFile.mkdirs();
		String filename = fileName.substring(0, fileName.indexOf('.'));
		String filepath = outDir + filename + "-CFG.dot";
		try (PrintWriter dot = new PrintWriter(filepath, "UTF-8")) {
			dot.println("digraph " + filename + "_CFG {");
            dot.println("  // graph-vertices");
			Map<CFNode, String> nodeNames = new LinkedHashMap<>();
			int nodeCounter = 1;
			for (CFNode node: allVertices) {
				String name = "v" + nodeCounter++;
				nodeNames.put(node, name);
				StringBuilder label = new StringBuilder("  [label=\"");
				if (node.getLineOfCode() > 0)
					label.append(node.getLineOfCode()).append(":  ");
				label.append(StringUtils.escape(node.getCode())).append("\"];");
				dot.println("  " + name + label.toString());
			}
			dot.println("  // graph-edges");
			for (Edge<CFNode, CFEdge> edge: allEdges) {
				String src = nodeNames.get(edge.source);
				String trg = nodeNames.get(edge.target);
				if (edge.label.type.equals(CFEdge.Type.EPSILON))
					dot.println("  " + src + " -> " + trg + ";");
				else
					dot.println("  " + src + " -> " + trg + "  [label=\"" + edge.label.type + "\"];");
			}
			dot.println("  // end-of-graph\n}");
		} catch (UnsupportedEncodingException ex) {
			Logger.error(ex);
		}
		Logger.info("CFG exported to: " + filepath);
	}	

    @Override
    public void exportGML(String outDir) throws IOException {
        if (!outDir.endsWith(File.separator))
            outDir += File.separator;
        File outDirFile = new File(outDir);
        outDirFile.mkdirs();
		String filename = fileName.substring(0, fileName.indexOf('.'));
		String filepath = outDir + filename + "-CFG.gml";
		try (PrintWriter gml = new PrintWriter(filepath, "UTF-8")) {
			gml.println("graph [");
			gml.println("  directed 1");
			gml.println("  multigraph 1");
			for (Entry<String, String> property: properties.entrySet()) {
                switch (property.getKey()) {
                    case "directed":
                        continue;
                    default:
                        gml.println("  " + property.getKey() + " \"" + property.getValue() + "\"");
                }
            }
            gml.println("  file \"" + this.fileName + "\"");
            gml.println("  package \"" + this.pkgName + "\"\n");
            //
			Map<CFNode, Integer> nodeIDs = new LinkedHashMap<>();
			int nodeCounter = 0;
			for (CFNode node: allVertices) {
				gml.println("  node [");
				gml.println("    id " + nodeCounter);
				gml.println("    line " + node.getLineOfCode());
				gml.println("    label \"" + StringUtils.escape(node.getCode()) + "\"");
				gml.println("  ]");
				nodeIDs.put(node, nodeCounter);
				++nodeCounter;
			}
            gml.println();
            //
			int edgeCounter = 0;
			for (Edge<CFNode, CFEdge> edge: allEdges) {
				gml.println("  edge [");
				gml.println("    id " + edgeCounter);
				gml.println("    source " + nodeIDs.get(edge.source));
				gml.println("    target " + nodeIDs.get(edge.target));
				gml.println("    label \"" + edge.label.type + "\"");
				gml.println("  ]");
				++edgeCounter;
			}
			gml.println("]");
		} catch (UnsupportedEncodingException ex) {
			Logger.error(ex);
		}
		Logger.info("CFG exported to: " + filepath);
    }
	
    @Override
	public void exportJSON(String outDir) throws FileNotFoundException {
        if (!outDir.endsWith(File.separator))
            outDir += File.separator;
        File outDirFile = new File(outDir);
        outDirFile.mkdirs();
		String filename = fileName.substring(0, fileName.indexOf('.'));
		String filepath = outDir + filename + "-CFG.json";
		try (PrintWriter json = new PrintWriter(filepath, "UTF-8")) {
			json.println("{\n  \"directed\": true,");
			json.println("  \"multigraph\": true,");
			for (Entry<String, String> property: properties.entrySet()) {
                switch (property.getKey()) {
                    case "directed":
                        continue;
                    default:
                        json.println("  \"" + property.getKey() + "\": \"" + property.getValue() + "\",");
                }
            }
			json.println("  \"file\": \"" + fileName + "\",");
            json.println("  \"package\": \"" + this.pkgName + "\",\n");
            //
			json.println("  \"nodes\": [");
			Map<CFNode, Integer> nodeIDs = new LinkedHashMap<>();
			int nodeCounter = 0;
			for (CFNode node: allVertices) {
                json.println("    {");
				json.println("      \"id\": " + nodeCounter + ",");
				json.println("      \"line\": " + node.getLineOfCode() + ",");
				json.println("      \"label\": \"" + StringUtils.escape(node.getCode()) + "\"");
				nodeIDs.put(node, nodeCounter);
				++nodeCounter;
                if (nodeCounter == allVertices.size())
                    json.println("    }");
                else
                    json.println("    },");
			}
            //
			json.println("  ],\n\n  \"edges\": [");
			int edgeCounter = 0;
			for (Edge<CFNode, CFEdge> edge: allEdges) {
				json.println("    {");
				json.println("      \"id\": " + edgeCounter + ",");
				json.println("      \"source\": " + nodeIDs.get(edge.source) + ",");
				json.println("      \"target\": " + nodeIDs.get(edge.target) + ",");
				json.println("      \"label\": \"" + edge.label.type + "\"");
				++edgeCounter;
                if (edgeCounter == allEdges.size())
                    json.println("    }");
                else
                    json.println("    },");
			}
			json.println("  ]\n}");
		} catch (UnsupportedEncodingException ex) {
			Logger.error(ex);
		}
		Logger.info("CFG exported to: " + filepath);
	}
}
