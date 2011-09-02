package org.codehaus.groovy.grails.web.binding

import grails.test.mixin.TestFor
import grails.persistence.Entity
import grails.test.mixin.Mock

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 8/29/11
 * Time: 10:55 AM
 * To change this template use File | Settings | File Templates.
 */

@TestFor(PersonController)
@Mock([TargetPerson, Book])
class BindXmlWithAssociationTests {

    void testBindXmlWithAssociatedId() {
        request.xml = '''
<person><name>xyz</name><book id='1'></book></person>
'''

        controller.save()

        assert response.text == 'saved'
    }

    void testBindXmlWithAssociatedIdAndProperties() {
        request.xml = '''
<person><name>xyz</name><book id='1'><title>Blah</title><pages>300</pages></book></person>
'''

        controller.save()

        assert response.text == 'saved'

        TargetPerson person = request.person

        assert person != null
        assert person.name == 'xyz'
        assert person.book != null
        assert person.book.title == 'Blah'
        assert person.book.pages == 300
    }
}
class PersonController {

    def save = {
        def person = new TargetPerson(params['person'])

        // uncomment next line to avoid error
        //person.merge()
        person.save(failOnError:true)

        request.person = person
        render 'saved'
    }

}

@Entity
class TargetPerson {

    String name
    Book book

}

@Entity
class Book {

    String title
    int pages
}
