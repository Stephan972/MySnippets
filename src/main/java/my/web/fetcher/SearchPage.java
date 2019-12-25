package my.web.fetcher;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import lombok.Getter;
import lombok.Setter;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class SearchPage {
	@XmlElement(name="url")
	@Setter
	@Getter
	private String url;
	
	@XmlElement(name = "result-css-path")
	@Setter
	@Getter
	private String resultCssPath;
	
	@XmlElement(name = "next-page")
	@Setter
	@Getter
	private String nextPage;	
}
