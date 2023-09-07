package org.pentaho.platform;

import org.pentaho.platform.api.scheduler2.ComplexJobTrigger;
import org.pentaho.platform.api.scheduler2.CronJobTrigger;
import org.pentaho.platform.api.scheduler2.SimpleJobTrigger;
import org.pentaho.platform.scheduler2.blockout.BlockoutAction;

import javax.ws.rs.Produces;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;

/**
 * Provide a customized JAXBContext that makes the concrete implementations
 * known and available for marshalling
 *
 * @author Michael Irwin
 */
@Provider
//@Produces({"application/xml", "application/json"})
public class JaxbContextResolver implements ContextResolver<JAXBContext> {

  private final JAXBContext jaxbContext;

  public JaxbContextResolver() {
    try {
      jaxbContext =
        JAXBContext.newInstance( SimpleJobTrigger.class, CronJobTrigger.class, ComplexJobTrigger.class, BlockoutAction.class );
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public JAXBContext getContext(Class<?> clazz) {
    return jaxbContext;
  }

}
