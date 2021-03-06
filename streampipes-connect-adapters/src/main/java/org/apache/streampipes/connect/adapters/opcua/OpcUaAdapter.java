/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.streampipes.connect.adapters.opcua;

import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.apache.streampipes.connect.adapter.Adapter;
import org.apache.streampipes.connect.adapter.exception.AdapterException;
import org.apache.streampipes.connect.adapter.exception.ParseException;
import org.apache.streampipes.connect.adapter.model.specific.SpecificDataStreamAdapter;
import org.apache.streampipes.connect.adapter.sdk.ParameterExtractor;
import org.apache.streampipes.container.api.ResolvesContainerProvidedOptions;
import org.apache.streampipes.model.AdapterType;
import org.apache.streampipes.model.connect.adapter.SpecificAdapterStreamDescription;
import org.apache.streampipes.model.connect.guess.GuessSchema;
import org.apache.streampipes.model.schema.EventProperty;
import org.apache.streampipes.model.schema.EventSchema;
import org.apache.streampipes.sdk.StaticProperties;
import org.apache.streampipes.sdk.builder.PrimitivePropertyBuilder;
import org.apache.streampipes.sdk.builder.adapter.SpecificDataStreamAdapterBuilder;
import org.apache.streampipes.sdk.extractor.StaticPropertyExtractor;
import org.apache.streampipes.sdk.helpers.Alternatives;
import org.apache.streampipes.sdk.helpers.Labels;

import java.util.*;

public class OpcUaAdapter extends SpecificDataStreamAdapter {

    public static final String ID = "org.apache.streampipes.connect.adapters.opcua";

    private static final String OPC_HOST_OR_URL = "OPC_HOST_OR_URL";
    private static final String OPC_URL = "OPC_URL";
    private static final String OPC_HOST = "OPC_HOST";
    private static final String OPC_SERVER_URL = "OPC_SERVER_URL";
    private static final String OPC_SERVER_HOST = "OPC_SERVER_HOST";
    private static final String OPC_SERVER_PORT = "OPC_SERVER_PORT";
    private static final String NAMESPACE_INDEX = "NAMESPACE_INDEX";
    private static final String NODE_ID = "NODE_ID";

    private String opcUaServer;
    private String namespaceIndex;
    private String nodeId;
    private String port;
    private boolean selectedURL;

    private Map<String, Object> event;

    private OpcUa opcUa;

    private int numberProperties;


    public OpcUaAdapter() {
        this.event = new HashMap<>();
        this.numberProperties = 0;
    }

    public OpcUaAdapter(SpecificAdapterStreamDescription adapterDescription) {
        super(adapterDescription);

        getConfigurations(adapterDescription);

        this.event = new HashMap<>();
        this.numberProperties = 0;
    }

    @Override
    public SpecificAdapterStreamDescription declareModel() {

        SpecificAdapterStreamDescription description = SpecificDataStreamAdapterBuilder.create(ID, "OPC UA", "Reads values from an OPC-UA server")
                .iconUrl("opc.png")
                .category(AdapterType.Generic, AdapterType.Manufacturing)
                .requiredAlternatives(Labels.from(OPC_HOST_OR_URL, "OPC Server", ""),
                        Alternatives.from(Labels.from(OPC_URL, "URL", ""),
                                StaticProperties.stringFreeTextProperty(Labels.from(OPC_SERVER_URL, "URL", "Example: opc.tcp://test-server.com:4840"))
                        ),
                        Alternatives.from(Labels.from(OPC_HOST, "Host/Port", ""),
                                StaticProperties.group(Labels.withId("host-port"),
                                        StaticProperties.stringFreeTextProperty(Labels.from(OPC_SERVER_HOST, "Host", "Example: test-server.com (No leading opc.tcp://) ")),
                                        StaticProperties.stringFreeTextProperty(Labels.from(OPC_SERVER_PORT, "Port", "Example: 4840")))))
                .requiredTextParameter(Labels.from(NAMESPACE_INDEX, "Namespace Index", "Example: 2"))
                .requiredTextParameter(Labels.from(NODE_ID, "Node Id", "Id of the Node to read the values from"))
                .build();
        description.setAppId(ID);


        return  description;
    }

    public void onSubscriptionValue(UaMonitoredItem item, DataValue value) {

        String key = getRuntimeNameOfNode(item.getReadValueId().getNodeId());

        event.put(key, value.getValue().getValue());

        // ensure that event is complete and all opc ua subscriptions transmitted at least one value
        if (event.keySet().size() >= this.numberProperties) {
            adapterPipeline.process(event);
        }
    }


    @Override
    public void startAdapter() throws AdapterException {
        if (this.selectedURL) {
            this.opcUa = new OpcUa(opcUaServer, Integer.parseInt(namespaceIndex), nodeId);
        } else {
            this.opcUa = new OpcUa(opcUaServer, Integer.parseInt(port), Integer.parseInt(namespaceIndex), nodeId);
        }

        try {
            this.opcUa.connect();

            List<OpcNode> allNodes = this.opcUa.browseNode();
            List<NodeId> nodeIds = new ArrayList<>();

            for (OpcNode rd : allNodes) {
                nodeIds.add(rd.nodeId);
            }

            this.numberProperties = nodeIds.size();
            this.opcUa.createListSubscription(nodeIds, this);
        } catch (Exception e) {
            throw new AdapterException("Could not connect to OPC-UA server! Server: " + opcUaServer + " Port: " + port +
                    " NamespaceIndex: " + namespaceIndex + " NodeId: " + nodeId);
        }
    }

    @Override
    public void stopAdapter() throws AdapterException {
        // close connection
        this.opcUa.disconnect();
    }

    @Override
    public Adapter getInstance(SpecificAdapterStreamDescription adapterDescription) {
        return new OpcUaAdapter(adapterDescription);
    }

    @Override
    public GuessSchema getSchema(SpecificAdapterStreamDescription adapterDescription) throws AdapterException, ParseException {

        GuessSchema guessSchema = new GuessSchema();
        EventSchema eventSchema = new EventSchema();
        List<EventProperty> allProperties = new ArrayList<>();


        getConfigurations(adapterDescription);
        OpcUa opc;
        if (this.selectedURL) {
            opc = new OpcUa(opcUaServer, Integer.parseInt(namespaceIndex), nodeId);
        } else {
            opc = new OpcUa(opcUaServer, Integer.parseInt(port), Integer.parseInt(namespaceIndex), nodeId);
        }

        try {
            opc.connect();
            List<OpcNode> res =  opc.browseNode();


            if (res.size() > 0) {
                for (OpcNode opcNode : res) {

                    String runtimeName = getRuntimeNameOfNode(opcNode.getNodeId());
                    allProperties.add(PrimitivePropertyBuilder
                            .create(opcNode.getType(), runtimeName)
                            .label(opcNode.getLabel())
                            .build());
                }
            }

            opc.disconnect();
        } catch (Exception e) {

            throw new AdapterException("Could not guess schema for opc node! " + e.getMessage());

        }

        eventSchema.setEventProperties(allProperties);
        guessSchema.setEventSchema(eventSchema);

        return guessSchema;
    }

    @Override
    public String getId() {
        return ID;
    }

    private void getConfigurations(SpecificAdapterStreamDescription adapterDescription) {
        StaticPropertyExtractor extractor =
                StaticPropertyExtractor.from(adapterDescription.getConfig(), new ArrayList<>());

        String selectedAlternative = extractor.selectedAlternativeInternalId(OPC_HOST_OR_URL);

        if (selectedAlternative.equals(OPC_URL)) {
            this.opcUaServer = extractor.singleValueParameter(OPC_SERVER_URL, String.class);
            this.selectedURL = true;
        } else {
            this.opcUaServer = extractor.singleValueParameter(OPC_SERVER_HOST, String.class);
            this.port = extractor.singleValueParameter(OPC_SERVER_PORT, String.class);
            this.selectedURL = false;
        }

        this.namespaceIndex = extractor.singleValueParameter(NAMESPACE_INDEX, String.class);
        this.nodeId = extractor.singleValueParameter(NODE_ID, String.class);
    }

//    @Override
//    public List<Option> resolveOptions(String requestId, StaticPropertyExtractor parameterExtractor) {
//        String opcUaServer = parameterExtractor.singleValueParameter(OPC_SERVER_HOST, String.class);
//        int port = parameterExtractor.singleValueParameter(OPC_SERVER_PORT, Integer.class);
//        int namespaceIndex = parameterExtractor.singleValueParameter(NAMESPACE_INDEX, Integer.class);
//
//        OpcUa opc = new OpcUa(opcUaServer, port, namespaceIndex, Identifiers.RootFolder);
//
//        try {
//            opc.connect();
//            List<OpcNode> res =  opc.browseNode();
//            System.out.println(res);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return new ArrayList<>();
//    }


    private String getRuntimeNameOfNode(NodeId nodeId) {
        String[] keys = nodeId.getIdentifier().toString().split("\\.");
        String key;

        if (keys.length > 0) {
            key = keys[keys.length - 1];
        } else {
            key = nodeId.getIdentifier().toString();
        }

        return key;
    }
}
