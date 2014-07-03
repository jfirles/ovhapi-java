# ovhapi-java

Very simple Java Helper for OVH REST API

## Install

* Clone repository or download zip and unzip it
`wget https://github.com/jfirles/ovhapi-java/archive/master.zip && unzip master.zip`
* Compile and install with maven
`cd ovhapi-java-master && mvn clean install`
* Add dependency to your project
```
<dependency>
  <groupId>com.siptize</groupId>
  <artifactId>ovhapi</artifactId>
  <version>1.0.0</version>
</dependency>
```

## Usage

You need an `applicationKey`, `appSecret` and an `applicationConsumerKey` to use it, which can be obtained [there](https://eu.api.ovh.com/createApp/).

```
import com.siptize.ovhapi.OvhApi;

package default;

public class TestOvhApi {
	public static main(String[] args) throws Exception {
		// credentials
		String appKey = "7kbG7Bk7S9Nt7ZSV";
		String appSecret = "EXEgWIz07P0HYwtQDs7cNIqCiQaWSuHF";
		String consumerKey = "MtSwSrPpNjqfVSmJhLbPyr2i45lSwPU1";
		// intance api
		OvhApi ovhApi = new OvhApi(OvhApi.OVH_API_EU_BASE_URL, appKey, appSecret, consumerKey); // EU
//		OvhApi ovhApi = new OvhApi(OvhApi.OVH_API_CA_BASE_URL, appKey, appSecret, consumerKey); // CA
//		OvhApi ovhApi = new OvhApi("https://otherapiurlforovh.com/api/rest", appKey, appSecret, consumerKey); // Other

		// get request
		String jsonResponse = ovhApi.get("/domain);
		// show current domains
		System.out.println("Current domains in json:\n" + jsonResponse);

		// post request
		// json with params
		String bodyCreateARecord = "{\"fieldType\":\"A\", ... }";
		jsonResponse = ovhApi.post("/domain/zone/myzone.com/record", bodyCreateARecord);
		System.out.println("Response creating zone");

		// put
		// json with params
		String bodyUpdate = "{ ... }";
		jsonResponse = ovhApi.put("/domain/zone/myzone.com/record", bodyUpdate);
		System.out.println("Response updating zone");

		// delete
		String idToDelete = "905684"
		jsonResponse = ovhApi.delete("/domain/zone/myzone.com/record/" + idToDelete);
		System.out.println("Response deleting zone");
		
	}
}
```
