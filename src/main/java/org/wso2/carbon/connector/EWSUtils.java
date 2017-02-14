/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.connector;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.impl.jaxp.OMSource;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.synapse.MessageContext;
import org.wso2.carbon.connector.core.util.ConnectorUtils;
import org.wso2.carbon.utils.xml.StringUtils;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;

/**
 * Utility functions for EWS Connector
 */
class EWSUtils {
    private static final String XSLT_FILE_LOCATION = "xslt/namespace.xslt";
    private static SOAPFactory soapFactory = OMAbstractFactory.getSOAP11Factory();
    static OMNamespace type = soapFactory.createOMNamespace(EWSConstants.TYPE_NAME_SPACE, EWSConstants
            .TYPE_NAME_SPACE_HEADER);
    static OMNamespace message = soapFactory.createOMNamespace(EWSConstants.MESSAGES_NAME_SPACE, EWSConstants
            .MESSAGE_NAME_SPACE_HEADER);

    /**
     * used to Create OmAttribute and set value
     *
     * @param messageContext messageContext of Request
     * @param rootElement rootElement to set Attribute
     * @param templateParameter template parameter name
     * @param attributeKey attribute name
     */
    static boolean setValueToXMLAttribute(MessageContext messageContext, OMElement rootElement, String
            templateParameter, String attributeKey) {
        String value = (String) ConnectorUtils.lookupTemplateParamater(messageContext, templateParameter);
        if (!StringUtils.isEmpty(value)) {
            rootElement.addAttribute(soapFactory.createOMAttribute(attributeKey, null, value));
            return true;
        }
        return false;
    }

    /**
     * Used to populate complex Elements with name spaces
     *
     * @param messageContext messageContext of Request
     * @param baseElement base Element to set element
     * @param parameterName parameter name as config
     * @throws XMLStreamException
     * @throws TransformerException when transformation failed
     */
    static void populateDirectElements(MessageContext messageContext, OMElement baseElement, String
            parameterName) throws XMLStreamException, TransformerException {
        String parametrisedValue = (String) ConnectorUtils.lookupTemplateParamater(messageContext, parameterName);
        if (!StringUtils.isEmpty(parametrisedValue)) {
            OMElement element = AXIOMUtil.stringToOM(parametrisedValue);
            baseElement.addChild(setNameSpaceForElements(element));
        }
    }

    /**
     * Used to populate complex Elements with name spaces
     *
     * @param messageContext messageContext of Request
     * @param baseElement base Element to set element
     * @param parameterName parameter name as config
     * @param rootNameSpace nameSpace for Root Element
     * @throws XMLStreamException
     * @throws TransformerException when transformation get failed
     */
    static void populateDirectElements(MessageContext messageContext, OMElement baseElement, String
            parameterName, OMNamespace rootNameSpace) throws XMLStreamException, TransformerException {
        String parametrisedValue = (String) ConnectorUtils.lookupTemplateParamater(messageContext, parameterName);
        if (!StringUtils.isEmpty(parametrisedValue)) {
            OMElement element = AXIOMUtil.stringToOM(parametrisedValue);
            OMElement nameSpaceAwareElement = setNameSpaceForElements(element);
            nameSpaceAwareElement.setNamespace(rootNameSpace);
            baseElement.addChild(nameSpaceAwareElement);
        }
    }

    /**
     * Used to populate <TimeZoneDefinition><TimeZoneDefinition/> element
     *
     * @param messageContext messageContext of Request
     * @param timeZoneContextHeader timezoneContext Header Omelement
     * @throws XMLStreamException
     * @throws TransformerException when transformation get failed
     */
    private static void populateTimeZoneDefinitionHeader(MessageContext messageContext, OMElement
            timeZoneContextHeader) throws XMLStreamException, TransformerException {
        String timeZoneDefinitionObject = (String) ConnectorUtils.lookupTemplateParamater(messageContext,
                EWSConstants.TIME_ZONE_DEFINITION);
        OMElement timezoneDefinitionSoapElement = soapFactory.createOMElement(EWSConstants
                .TIME_ZONE_DEFINITION_HEADER, type);
        if (!StringUtils.isEmpty(timeZoneDefinitionObject)) {
            OMElement timezoneDefinitionElement = AXIOMUtil.stringToOM(timeZoneDefinitionObject);
            OMElement idElement = timezoneDefinitionElement.getFirstChildWithName(new QName(EWSConstants.ID_ATTRIBUTE));
            if (idElement != null) {
                timezoneDefinitionSoapElement.addAttribute(soapFactory.createOMAttribute(EWSConstants.ID_ATTRIBUTE,
                        null, idElement.getText()));
            }
            OMElement nameElement = timezoneDefinitionElement.getFirstChildWithName(new QName(EWSConstants
                    .NAME_ATTRIBUTE));
            if (nameElement != null) {
                timezoneDefinitionSoapElement.addAttribute(soapFactory.createOMAttribute(EWSConstants.NAME_ATTRIBUTE,
                        null, nameElement.getText()));
            }
        }
        populateDirectElements(messageContext, timezoneDefinitionSoapElement, EWSConstants.PERIODS);
        populateDirectElements(messageContext, timezoneDefinitionSoapElement, EWSConstants.TRANSITION_GROUPS);
        populateDirectElements(messageContext, timezoneDefinitionSoapElement, EWSConstants.TRANSITIONS);
        if (timezoneDefinitionSoapElement.getChildElements().hasNext()) {
            timeZoneContextHeader.addChild(timezoneDefinitionSoapElement);
        }
    }

    /**
     * used to populate <ExchangeImpersonation><ExchangeImpersonation/> element
     *
     * @param exchangeImpersonationSoapHeaderBlock <ExchangeImpersonation> root Element
     * @param messageContext messageContext of Request
     */
    private static void populateExchangeImpersonation(OMElement exchangeImpersonationSoapHeaderBlock,
                                                      MessageContext messageContext) {
        OMElement connectingSidOmElement = soapFactory.createOMElement(EWSConstants.CONNECTING_SID, type);
        setValueToXMLElement(messageContext, EWSConstants.PRINCIPAL_NAME, connectingSidOmElement, EWSConstants
                .PRINCIPAL_NAME_ELEMENT);
        setValueToXMLElement(messageContext, EWSConstants.SID, connectingSidOmElement, EWSConstants.SID_ELEMENT);
        setValueToXMLElement(messageContext, EWSConstants.PRIMARY_SMTP_ADDRESS, connectingSidOmElement, EWSConstants
                .PRIMARY_SMTP_ADDRESS_ELEMENT);
        setValueToXMLElement(messageContext, EWSConstants.SMTP_ADDRESS, connectingSidOmElement, EWSConstants
                .SMTP_ADDRESS_ELEMENT);
        if (connectingSidOmElement.getChildElements().hasNext()) {
            exchangeImpersonationSoapHeaderBlock.addChild(connectingSidOmElement);
        }
    }

    /**
     * Used to set OmElement value from connector configuration
     *
     * @param messageContext messageContext of Request
     * @param templateParameter  template parameter name
     * @param baseElement base Element to set element
     * @param elementName element Name for create
     */
    static void setValueToXMLElement(MessageContext messageContext, String templateParameter, OMElement
            baseElement, String elementName) {
        OMElement rootElement = soapFactory.createOMElement(elementName, type);
        String value = (String) ConnectorUtils.lookupTemplateParamater(messageContext, templateParameter);
        if (!StringUtils.isEmpty(value)) {
            rootElement.setText(value);
            baseElement.addChild(rootElement);
        }
    }

    /**
     * Used to set OmElement value from connector configuration
     *
     * @param messageContext messageContext of Request
     * @param templateParameter template parameter name
     * @param rootElement rootElement to set Attribute
     */
    private static boolean setValueToXMLElement(MessageContext messageContext, OMElement rootElement, String
            templateParameter) {
        String value = (String) ConnectorUtils.lookupTemplateParamater(messageContext, templateParameter);
        if (!StringUtils.isEmpty(value)) {
            rootElement.setText(value);
            return true;
        }
        return false;
    }

    /**
     * used to set namespace to Elements.
     *
     * @param element element to set nameSpaces
     * @return return namespace set OmElement
     * @throws TransformerException when transformation failed
     * @throws XMLStreamException
     */
    static OMElement setNameSpaceForElements(OMElement element) throws TransformerException, XMLStreamException {
        Source xmlSource = new OMSource(element);
        StreamSource xsltSource = new StreamSource(EWSUtils.class.getClassLoader().getResourceAsStream
                (XSLT_FILE_LOCATION));
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer(xsltSource);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        StreamResult result = new StreamResult(output);
        transformer.transform(xmlSource, result);
        return AXIOMUtil.stringToOM(new String(output.toByteArray()));
    }

    /**
     * used to populate <ItemIds></ItemIds> element
     *
     * @param messageContext messageContext of Request
     * @return ItemIds OmElement
     * @throws XMLStreamException
     * @throws TransformerException when transformation failed
     */
    static OMElement populateItemIds(MessageContext messageContext) throws XMLStreamException,
            TransformerException {
        OMElement itemIdsElement = soapFactory.createOMElement(EWSConstants.ITEM_IDS, message);
        OMElement itemIdElement = AXIOMUtil.stringToOM((String) ConnectorUtils.lookupTemplateParamater
                (messageContext, EWSConstants.ITEM_ID));
        if (itemIdElement != null) {
            String id = itemIdElement.getFirstChildWithName(new QName(EWSConstants.ID_ATTRIBUTE)).getText();
            String changeKey = itemIdElement.getFirstChildWithName(new QName(EWSConstants.CHANGE_KEY_ATTRIBUTE))
                    .getText();
            OMElement itemIdOmElement = soapFactory.createOMElement(EWSConstants.ITEM_ID_ELEMENT, type);
            itemIdOmElement.addAttribute(soapFactory.createOMAttribute(EWSConstants.ID_ATTRIBUTE, null, id));
            itemIdOmElement.addAttribute(soapFactory.createOMAttribute(EWSConstants.CHANGE_KEY_ATTRIBUTE, null,
                    changeKey));
            itemIdsElement.addChild(itemIdOmElement);

        }
        String occurrenceItem = (String) ConnectorUtils.lookupTemplateParamater(messageContext, EWSConstants
                .OCCURRENCE_ITEM_ID);
        if (!StringUtils.isEmpty(occurrenceItem)) {
            OMElement occurrenceItemId = AXIOMUtil.stringToOM(occurrenceItem);
            String recurringMasterId = occurrenceItemId.getFirstChildWithName(new QName(EWSConstants
                    .RECURRING_MASTER_ID_ATTRIBUTE)).getText();
            String changeKey = occurrenceItemId.getFirstChildWithName(new QName(EWSConstants.CHANGE_KEY_ATTRIBUTE))
                    .getText();
            String instanceIndex = occurrenceItemId.getFirstChildWithName(new QName(EWSConstants
                    .INSTANCE_INDEX_ATTRIBUTE)).getText();
            OMElement itemIdOmElement = soapFactory.createOMElement(EWSConstants.OCCURENCE_ITEM_ID_ELEMENT, type);
            itemIdOmElement.addAttribute(soapFactory.createOMAttribute(EWSConstants.RECURRING_MASTER_ID_ATTRIBUTE,
                    null, recurringMasterId));
            itemIdOmElement.addAttribute(soapFactory.createOMAttribute(EWSConstants.CHANGE_KEY_ATTRIBUTE, null,
                    changeKey));
            itemIdOmElement.addAttribute(soapFactory.createOMAttribute(EWSConstants.INSTANCE_INDEX_ATTRIBUTE, null,
                    instanceIndex));
            itemIdsElement.addChild(itemIdOmElement);
        }
        String recurrenceItem = (String) ConnectorUtils.lookupTemplateParamater(messageContext, EWSConstants
                .RECURRENCE_MASTER_ITEM_ID);
        if (!StringUtils.isEmpty(recurrenceItem)) {
            OMElement recurrenceItemId = AXIOMUtil.stringToOM(recurrenceItem);
            String occurrenceId = recurrenceItemId.getFirstChildWithName(new QName(EWSConstants
                    .OCCURRENCE_ID_ATTRIBUTE)).getText();
            String changeKey = recurrenceItemId.getFirstChildWithName(new QName(EWSConstants.CHANGE_KEY_ATTRIBUTE))
                    .getText();
            OMElement itemIdOmElement = soapFactory.createOMElement(EWSConstants.RECURRENCE_MASTER_ITEM_ID_ELEMENT,
                    type);
            itemIdOmElement.addAttribute(soapFactory.createOMAttribute(EWSConstants.OCCURRENCE_ID_ATTRIBUTE, null,
                    occurrenceId));
            itemIdOmElement.addAttribute(soapFactory.createOMAttribute(EWSConstants.CHANGE_KEY_ATTRIBUTE, null,
                    changeKey));
            itemIdsElement.addChild(itemIdOmElement);
        }
        String recurringMasterItemIdRanges = (String) ConnectorUtils.lookupTemplateParamater(messageContext,
                EWSConstants.RECURRING_MASTER_ITEM_ID_RANGES);
        if (!StringUtils.isEmpty(recurringMasterItemIdRanges)) {
            OMElement recurringMasterItemIdRangesElement = AXIOMUtil.stringToOM(recurringMasterItemIdRanges);
            OMElement recurrenceMasterItemIdRangesElement = soapFactory.createOMElement(EWSConstants
                    .RECURRING_MASTER_ITEM_ID_RANGES_ELEMENT, type);
            String id = recurringMasterItemIdRangesElement.getFirstChildWithName(new QName(EWSConstants.ID_ATTRIBUTE))
                    .getText();
            String changeKey = recurringMasterItemIdRangesElement.getFirstChildWithName(new QName(EWSConstants
                    .CHANGE_KEY_ATTRIBUTE)).getText();
            recurrenceMasterItemIdRangesElement.addAttribute(soapFactory.createOMAttribute(EWSConstants
                    .ID_ATTRIBUTE, null, id));
            recurrenceMasterItemIdRangesElement.addAttribute(soapFactory.createOMAttribute(EWSConstants
                    .CHANGE_KEY_ATTRIBUTE, null, changeKey));
            OMElement rangesElement = recurringMasterItemIdRangesElement.getFirstChildWithName(new QName(EWSConstants
                    .RANGES_ELEMENT));
            recurrenceMasterItemIdRangesElement.addChild(setNameSpaceForElements(rangesElement));
            itemIdsElement.addChild(recurrenceMasterItemIdRangesElement);
        }
        return itemIdsElement;
    }

    /**
     * used to populate <SavedItemFolderId></SavedItemFolderId> element
     *
     * @param messageContext messageContext of Request
     * @param baseElement base Element to set element
     * @return true if element is available
     * @throws XMLStreamException
     * @throws TransformerException when transformation failed
     */
    static boolean populateSaveItemFolderIdElement(MessageContext messageContext, OMElement baseElement) throws
            XMLStreamException, TransformerException {
        String folderIdString = (String) ConnectorUtils.lookupTemplateParamater(messageContext, EWSConstants.FOLDER_ID);
        if (!StringUtils.isEmpty(folderIdString)) {
            OMElement folderIdElement = soapFactory.createOMElement(EWSConstants.FOLDER_ID_ELEMENT, type);
            OMElement folderElement = AXIOMUtil.stringToOM(folderIdString);
            folderIdElement.addAttribute(soapFactory.createOMAttribute(EWSConstants.ID_ATTRIBUTE, null, folderElement
                    .getFirstChildWithName(new QName(EWSConstants.ID_ATTRIBUTE)).getText()));
            folderIdElement.addAttribute(soapFactory.createOMAttribute(EWSConstants.CHANGE_KEY_ATTRIBUTE, null,
                    folderElement
                            .getFirstChildWithName(new QName(EWSConstants.CHANGE_KEY_ATTRIBUTE)).getText()));
            baseElement.addChild(folderIdElement);
        }
        String distinguishedId = (String) ConnectorUtils.lookupTemplateParamater(messageContext, EWSConstants
                .DISTINGUISHED_FOLDER_ID);
        OMElement distinguishedFolderIdOmElement = soapFactory.createOMElement(EWSConstants
                .DISTINGUISHED_FOLDER_ID_ELEMENT, type);
        if (!StringUtils.isEmpty(distinguishedId)) {
            OMElement folderElement = AXIOMUtil.stringToOM(distinguishedId);
            OMElement idElement = folderElement.getFirstChildWithName(new QName(EWSConstants.ID_ATTRIBUTE));
            if (idElement != null) {
                distinguishedFolderIdOmElement.addAttribute(soapFactory.createOMAttribute(EWSConstants.ID_ATTRIBUTE,
                        null,
                        idElement.getText()));
            }
            OMElement changeKeyElement = folderElement.getFirstChildWithName(new QName(EWSConstants
                    .CHANGE_KEY_ATTRIBUTE));
            if (changeKeyElement != null) {
                distinguishedFolderIdOmElement.addAttribute(soapFactory.createOMAttribute(EWSConstants
                                .CHANGE_KEY_ATTRIBUTE,
                        null, changeKeyElement.getText()));
            }
        }
        EWSUtils.populateDirectElements(messageContext, distinguishedFolderIdOmElement, EWSConstants.MAIL_BOX);
        if (distinguishedFolderIdOmElement.getChildElements().hasNext() && distinguishedFolderIdOmElement
                .getAllAttributes().hasNext()) {
            baseElement.addChild(distinguishedFolderIdOmElement);
        }
        OMElement addressListIdElement = soapFactory.createOMElement(EWSConstants.ADDRESS_LIST_ID_ELEMENT, type);
        EWSUtils.setValueToXMLAttribute(messageContext, addressListIdElement, EWSConstants.ADDRESS_LIST_ID, EWSConstants
                .ID_ATTRIBUTE);
        if (addressListIdElement.getAllAttributes().hasNext()){
            baseElement.addChild(addressListIdElement);
        }
        return baseElement.getChildElements().hasNext();
    }

    /**
     * used to populate <ItemShape></ItemShape> element
     *
     * @param messageContext messageContext of Request
     * @return ItemShape OmElement
     * @throws XMLStreamException
     * @throws TransformerException when transformation failed
     */
    static OMElement populateItemShape(MessageContext messageContext) throws XMLStreamException,
            TransformerException {
        OMElement itemShapeElement = soapFactory.createOMElement(EWSConstants.ITEM_SHAPE, message);
        setValueToXMLElement(messageContext, EWSConstants.BASE_SHAPE, itemShapeElement, EWSConstants
                .BASE_SHAPE_ELEMENT);
        setValueToXMLElement(messageContext, EWSConstants.INCLUDE_MIME_CONTENT, itemShapeElement, EWSConstants
                .INCLUDE_MIME_CONTENT_ELEMENT);
        setValueToXMLElement(messageContext, EWSConstants.BODY_TYPE, itemShapeElement, EWSConstants.BODY_TYPE_ELEMENT);
        setValueToXMLElement(messageContext, EWSConstants.UNIQUE_BODY_TYPE, itemShapeElement, EWSConstants
                .UNIQUE_BODY_TYPE_ELEMENT);
        setValueToXMLElement(messageContext, EWSConstants.NORMALIZED_BODY_TYPE, itemShapeElement, EWSConstants
                .NORMALIZED_BODY_TYPE_ELEMENT);
        setValueToXMLElement(messageContext, EWSConstants.FILTER_HTML_CONTENT, itemShapeElement, EWSConstants
                .FILTER_HTML_CONTENT_ELEMENT);
        setValueToXMLElement(messageContext, EWSConstants.CONVERT_HTML_TO_UTF8, itemShapeElement, EWSConstants
                .CONVERT_HTML_TO_UTF8_ELEMENT);
        setValueToXMLElement(messageContext, EWSConstants.INLINE_IMAGE_URL_TEMPLATE, itemShapeElement, EWSConstants
                .INLINE_IMAGE_URL_TEMPLATE_ELEMENT);
        setValueToXMLElement(messageContext, EWSConstants.ADD_BLANK_TARGET_TO_LINKS, itemShapeElement, EWSConstants
                .ADD_BLANK_TARGET_TO_LINKS_ELEMENT);
        setValueToXMLElement(messageContext, EWSConstants.MAXIMUM_BODY_SIZE, itemShapeElement, EWSConstants
                .MAXIMUM_BODY_SIZE_ELEMENT);
        String additionalProperties = (String) ConnectorUtils.lookupTemplateParamater(messageContext, EWSConstants
                .ADDITIONAL_PROPERTIES);
        if (!StringUtils.isEmpty(additionalProperties)) {
            OMElement additionalPropertiesElement = AXIOMUtil.stringToOM(additionalProperties);
            itemShapeElement.addChild(setNameSpaceForElements(additionalPropertiesElement));
        }
        return itemShapeElement;
    }

    /**
     * used to populate <ManagementRole></ManagementRole> element
     *
     * @param soapHeader soapHeader omElement
     * @param messageContext messageContext of Request
     * @throws TransformerException when transformation failed
     * @throws XMLStreamException
     */
    static void populateManagementRolesHeader(OMElement soapHeader, MessageContext messageContext) throws
            TransformerException, XMLStreamException {
        OMElement managementRoleHeader = soapFactory.createOMElement(EWSConstants.MANAGEMENT_ROLES_HEADER, type);
        EWSUtils.populateDirectElements(messageContext, managementRoleHeader, EWSConstants.USER_ROLES);
        EWSUtils.populateDirectElements(messageContext, managementRoleHeader, EWSConstants.APPLICATION_ROLES);
        if (managementRoleHeader.getChildElements().hasNext()) {
            soapHeader.addChild(managementRoleHeader);
        }
    }

    /**
     * used to populate <DateTimePrecision></DateTimePrecision> element
     *
     * @param soapHeader soapHeader omElement
     * @param messageContext messageContext of Request
     */
    static void populateDateTimePrecisionHeader(OMElement soapHeader, MessageContext messageContext) {
        OMElement dateTmePrecisionHeader = soapFactory.createOMElement(EWSConstants.DATE_TIME_PRECISION_HEADER, type);
        String dateTimePrecision = (String) ConnectorUtils.lookupTemplateParamater(messageContext, EWSConstants
                .DATE_TIME_PRECISION);
        if (!org.apache.commons.lang.StringUtils.isEmpty(dateTimePrecision)) {
            dateTmePrecisionHeader.setText(dateTimePrecision);
            soapHeader.addChild(dateTmePrecisionHeader);
        }
    }

    /**
     * used to populate <TimeZoneContext></TimeZoneContext> element
     *
     * @param soapHeader soapHeader omElement
     * @param messageContext messageContext of Request
     * @throws TransformerException when transformation failed
     * @throws XMLStreamException
     */
    static void populateTimeZoneContextHeader(OMElement soapHeader, MessageContext messageContext) throws
            TransformerException, XMLStreamException {
        OMElement timeZoneContextHeader = soapFactory.createOMElement(EWSConstants.TIME_ZONE_CONTEXT_HEADER, type);
        populateTimeZoneDefinitionHeader(messageContext, timeZoneContextHeader);
        if (timeZoneContextHeader.getChildElements().hasNext()) {
            soapHeader.addChild(timeZoneContextHeader);
        }
    }

    /**
     * used to populate <RequestServerVersion></RequestServerVersion> element
     *
     * @param soapHeader soapHeader omElement
     * @param messageContext messageContext of Request
     * @throws TransformerException when transformation failed
     * @throws XMLStreamException
     */
    static void populateRequestedServerVersionHeader(OMElement soapHeader, MessageContext messageContext) throws
            TransformerException, XMLStreamException {
        OMElement requestedServerVersionHeader = soapFactory.createOMElement(EWSConstants
                .REQUESTED_SERVER_VERSION_HEADER, type);
        if (EWSUtils.setValueToXMLAttribute(messageContext, requestedServerVersionHeader, EWSConstants
                .REQUESTED_SERVER_VERSION, EWSConstants.VERSION_ATTRIBUTE)) {
            soapHeader.addChild(requestedServerVersionHeader);
        }
    }

    /**
     * used to populate <MailBoxCulture></MailBoxCulture> element
     *
     * @param soapHeader soapHeader omElement
     * @param messageContext messageContext of Request
     */
    static void populateMailboxCulture(OMElement soapHeader, MessageContext messageContext) {
        OMElement mailBoxCultureHeader = soapFactory.createOMElement(EWSConstants.MAIL_BOX_CULTURE_HEADER, type);
        if (setValueToXMLElement(messageContext, mailBoxCultureHeader, EWSConstants.MAIL_BOX_CULTURE)) {
            soapHeader.addChild(mailBoxCultureHeader);
        }
    }

    /**
     * used to populate <ExchangeImpersonationHeader></ExchangeImpersonationHeader> element
     *
     * @param soapHeader soapHeader omElement
     * @param messageContext messageContext of Request
     */
    static void populateExchangeImpersonationHeader(OMElement soapHeader, MessageContext messageContext) {
        OMElement exchangeImpersonationSoapHeaderBlock = soapFactory.createOMElement(EWSConstants
                .EXCHANGE_IMPERSONATION_HEADER, type);
        EWSUtils.populateExchangeImpersonation(exchangeImpersonationSoapHeaderBlock, messageContext);
        if (exchangeImpersonationSoapHeaderBlock.getChildElements().hasNext()) {
            soapHeader.addChild(exchangeImpersonationSoapHeaderBlock);
        }

    }
}
