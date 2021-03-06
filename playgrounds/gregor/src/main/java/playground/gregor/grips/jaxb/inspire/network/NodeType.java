//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.08.04 at 02:05:46 PM CEST 
//


package playground.gregor.grips.jaxb.inspire.network;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import net.opengis.gml.v_3_2_1.PointPropertyType;
import net.opengis.gml.v_3_2_1.ReferenceType;
import playground.gregor.grips.jaxb.inspire.commontransportelements.TransportNodeType;


/**
 * <p>Java class for NodeType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="NodeType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:x-inspire:specification:gmlas:Network:3.2}NetworkElementType">
 *       &lt;sequence>
 *         &lt;element name="geometry" type="{http://www.opengis.net/gml/3.2}PointPropertyType"/>
 *         &lt;element name="spokeEnd" type="{http://www.opengis.net/gml/3.2}ReferenceType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="spokeStart" type="{http://www.opengis.net/gml/3.2}ReferenceType" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "NodeType", propOrder = {
    "geometry",
    "spokeEnd",
    "spokeStart"
})
@XmlSeeAlso({
    TransportNodeType.class
})
public abstract class NodeType
    extends NetworkElementType
{

    @XmlElement(required = true)
    protected PointPropertyType geometry;
    @XmlElement(nillable = true)
    protected List<ReferenceType> spokeEnd;
    @XmlElement(nillable = true)
    protected List<ReferenceType> spokeStart;

    /**
     * Gets the value of the geometry property.
     * 
     * @return
     *     possible object is
     *     {@link PointPropertyType }
     *     
     */
    public PointPropertyType getGeometry() {
        return geometry;
    }

    /**
     * Sets the value of the geometry property.
     * 
     * @param value
     *     allowed object is
     *     {@link PointPropertyType }
     *     
     */
    public void setGeometry(PointPropertyType value) {
        this.geometry = value;
    }

    public boolean isSetGeometry() {
        return (this.geometry!= null);
    }

    /**
     * Gets the value of the spokeEnd property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the spokeEnd property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSpokeEnd().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ReferenceType }
     * 
     * 
     */
    public List<ReferenceType> getSpokeEnd() {
        if (spokeEnd == null) {
            spokeEnd = new ArrayList<ReferenceType>();
        }
        return this.spokeEnd;
    }

    public boolean isSetSpokeEnd() {
        return ((this.spokeEnd!= null)&&(!this.spokeEnd.isEmpty()));
    }

    public void unsetSpokeEnd() {
        this.spokeEnd = null;
    }

    /**
     * Gets the value of the spokeStart property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the spokeStart property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSpokeStart().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ReferenceType }
     * 
     * 
     */
    public List<ReferenceType> getSpokeStart() {
        if (spokeStart == null) {
            spokeStart = new ArrayList<ReferenceType>();
        }
        return this.spokeStart;
    }

    public boolean isSetSpokeStart() {
        return ((this.spokeStart!= null)&&(!this.spokeStart.isEmpty()));
    }

    public void unsetSpokeStart() {
        this.spokeStart = null;
    }

}
