/*===========================================================================
 
  MyRouteBuilder.java

  This class defines the Camel routes for quarkus_suntimes.

  Copyright (c)2022 Kevin Boone, GPL v3.0

===========================================================================*/

package me.kevinboone.apacheintegration.quarkus_suntimes;

import org.apache.camel.builder.RouteBuilder;
import io.quarkus.runtime.StartupEvent;
import javax.enterprise.event.Observes;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import me.kevinboone.suntimes.SunTimesException; 
import org.apache.camel.model.rest.RestBindingMode;


public class MyRouteBuilder extends RouteBuilder 
  {
  /*
  // startup() could be a good place to invoke the RouteController
  //   to start routes that are defined in XML files. At present,
  //   there aren't any.
  void startup (@Observes StartupEvent event, CamelContext context) 
      throws Exception 
    {
    context.getRouteController().startAllRoutes();
    }
  */

  /** 
  */
  public void configure() throws Exception 
    {
    restConfiguration().bindingMode (RestBindingMode.json);

    /* Use an exception handler, to augument the default error response,
       which is a 400 with an empty body. Since there are various ways
       in which the Sunrise/set time calculation can fail, it would be
       helpful to the consumer of the service to see an actual reason. */
    onException (SunTimesException.class)
      .handled(true)
      .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
      .setHeader(Exchange.CONTENT_TYPE, constant("text/plain"))
      .setBody().simple
         ("Error calculating rise/set times: ${exception.message}");
   
    /* We will handle request URLs of the form /local/city/date or
       /local/city (with default date). Although the same Java code
       will handle both forms, we need to specify the different
       patterns here. We're using the Java "REST DSL" here -- these
       builder calls are not the same as those for the 
       route builder propoer. 

       The placeholders for city and date will end up as exchange
       headers, which might be null (and we have to deal with that) 

       The "local" in the URL reflects the fact that the calculated
       times are local to the specified city. We could provide other
       time formats, e.g., UTC times. */ 

    rest("/suntimes/local/{city}/{date}")
       .get()
       .to ("direct:local");

    rest("/health")
       .get()
       .to ("direct:health");

    rest("/suntimes/local/{city}")
       .get()
       .to ("direct:local");

    rest("/suntimes/list")
       .get()
       .to ("direct:list");

    /* Use a Camel method binding expression to invoke the method
       SunTimesBean.calculate() with the client's arguments. */
    from ("direct:local")
       .bean (new SunTimesBean(), 
         "calculate(${header.city}, ${header.date})"); 

    /* Use a Camel method binding expression to invoke the method
       SunTimesBean.list() */
    from ("direct:list")
       .bean (new SunTimesBean(), 
         "list()"); 

    /* Respond go the /health endpoint by returning a fixed string */
    /* Note that, unless we set the content-type header here, Camel will
       assume JSON, as this is a REST endpoint. */ 
    from ("direct:health")
       .setHeader(Exchange.CONTENT_TYPE, constant("text/plain")) 
       .setBody().constant ("OK\r\n");
    }
  }

