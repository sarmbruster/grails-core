package org.codehaus.groovy.grails.web.binding

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests
import org.codehaus.groovy.grails.web.mime.MimeType
import org.springframework.web.context.request.RequestContextHolder
import grails.test.mixin.TestFor
import spock.lang.Specification
import grails.converters.JSON

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 10/17/11
 * Time: 2:50 PM
 * To change this template use File | Settings | File Templates.
 */
@TestFor(ArrayController)
class JSONRequestToResponseRenderingSpec extends Specification{

    def "Test that JSON arrays are correctly converted in controllers"() {
        given:"A JSON request containing arrays"
            request.json = '''
            {
	"track": {
		"start_time": 1316975696560,
		"segments": [
			{
				"coordinates": [
					[
						47.8897441833333,
						-122.732959033333,
						101.1,
						1316975697100
					],
					[
						47.8898427833333,
						-122.732921583333,
						109.4,
						1316975704100
					]
				]
			}
		]
	}
}
'''
        when:"The params are rendered as JSON"
            controller.list()
        then:"Check that the JSON is convereted back correctly"
            response.json.track.segments != null
    }


}
class ArrayController {
    def list() {
        def json = request.JSON

        render json
    }
}
