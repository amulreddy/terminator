package com.autowares.mongoose.vipersoap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "PutawayUpdateResponse", namespace = "urn:kcml-Mongoose")
public class PutawayUpdateResponse {
	
	private String result;

    @XmlElement(name = "return")
    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
