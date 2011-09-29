

package br.uff.midiacom.axt.link;

import br.uff.midiacom.axt.connector.XTPConnector;
import AXT.XMLElement;
import AXT.XTPDoc;
import br.uff.midiacom.ana.connector.NCLCausalConnector;
import br.uff.midiacom.ana.datatype.NCLParamInstance;
import br.uff.midiacom.ana.descriptor.NCLLayoutDescriptor;
import br.uff.midiacom.ana.link.NCLBind;
import br.uff.midiacom.ana.link.NCLLink;
import br.uff.midiacom.ana.link.NCLParam;
import org.xml.sax.Attributes;
import org.xml.sax.XMLReader;

public class XTPLink<B extends NCLBind, P extends NCLParam, Cn extends XTPConnector,
        D extends NCLLayoutDescriptor, C extends NCLCausalConnector> extends NCLLink{

    private XTPConnector xtype;

    

    //construtores

     public XTPLink(){};

     public XTPLink(XMLReader reader, XMLElement parent) {
        super();
        setReader(reader);
        setParent(parent);

        getReader().setContentHandler(this);
    }

     //metodos de acesso

    public XTPConnector getXType(){
        return this.xtype;
    }

    public void setXType(XTPConnector xtype){
        this.xtype = xtype;
    }

   
    
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        try{
            if(localName.equals("link")){
                cleanWarnings();
                cleanErrors();
                for(int i = 0; i < attributes.getLength(); i++){
                    if(attributes.getLocalName(i).equals("id"))
                        setId(attributes.getValue(i));
                    //else if(attributes.getLocalName(i).equals("xconnector"))
                        //setXconnector((C) new NCLCausalConnector(attributes.getValue(i)));
                        //não é esperado achar um connector não declarado num link de xtemplate
                    // permitir a definição de elos com conectores não declarados?
                    else if(attributes.getLocalName(i).equals("xtype"))
                        setXType( new XTPConnector(attributes.getValue(i)));
                }
            }
            else if(localName.equals("linkParam")){
                P child = createLinkParam();
                child.startElement(uri, localName, qName, attributes);
                addLinkParam(child);
            }
            else if(localName.equals("bind")){
                B child = createBind();
                child.startElement(uri, localName, qName, attributes);
                addBind(child);
            }
        }
        catch(Exception ex){
            addError(ex.getMessage());
        }
    }


    @Override
    public void endDocument() {
        if(getParent() != null){
            if(getXconnector() != null)
                connectorReference();
        }

        if(hasLinkParam()){
            Iterable<P> linkParams = this.getLinkParams();
            for(P param : linkParams){
                param.endDocument();
                addWarning(param.getWarnings());
                addError(param.getErrors());
            }
        }
        if(hasBind()){
            Iterable<B> binds = this.getBinds();
            for(B bind : binds){
                bind.endDocument();
                addWarning(bind.getWarnings());
                addError(bind.getErrors());
            }
        }
    }


    private Iterable<Cn> getConnectors() {
        XMLElement root = (XMLElement) getParent();

        while(!(root instanceof XTPDoc)){
            root = (XMLElement) root.getParent();
            if(root == null){
                addWarning("Could not find a root element");
                return null;
            }
        }

        if(((XTPDoc) root).getVocabulary() == null){
            addWarning("Could not find a vocabulary");
            return null;
        }
        return ((XTPDoc) root).getVocabulary().getConnectors();
    }


    private void connectorReference() {
        //Search for the connector inside the base
        Iterable<Cn> connectors = getConnectors();
        if(connectors == null){
            addWarning("Could not find connector in vocabulary with xlabel: " + getXType().getXLabel());
            return;
        }

        for(Cn connector : connectors){
            if(connector.getXLabel().equals(this.getXType().getXLabel())){
                setXType(connector);
                return;
            }
        }

        addWarning("Could not find connector in vocabulary with id: " + getXType().getXLabel());
    }

    public void searchLink(Iterable<D> descriptors, Iterable<C> connectors){
        connectorReference();
        if(this.hasBind()){
        Iterable<B> binds = this.getBinds();
        for(B bind: binds){
          ((XTPBind) bind).bindSearch(descriptors);
        }
        }
        if(this.hasLinkParam()){
        Iterable<P> params = this.getLinkParams();
        for(P param: params){
            ((XTPParam)param).paramSearch();
        }
        }
    }

    @Override
    protected P createLinkParam() {
        return (P) new XTPParam(NCLParamInstance.LINKPARAM, getReader(), this);
    }

    @Override
    protected B createBind() {
        return (B) new XTPBind(getReader(), this);
    }
    @Override
    public String parse(int ident) {return null;}

    
    public boolean validate() {return true;}

    @Override
    public void addWarning(String warning) {}
}