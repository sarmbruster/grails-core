package grails.persistence

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class EntityTransformTests extends GroovyShellTestCase {

    // test for http://jira.codehaus.org/browse/GRAILS-5238
    void testGRAILS_5238() {
        def p = evaluate('''
import grails.persistence.*

@Entity
class Permission {
    String permission

    static belongsTo = [ user: User ]

    void setOwner(User owner) {
        this.user = owner
    }

    User getOwner() {
        return user
    }
}

@Entity
class User {
    String username
}

u = new User(username:"bob")
p = new Permission(user:u, permission:"uber")
''')

        assertEquals "User", p.user.class.name
        assertEquals "User", p.class.methods.find { it.name == 'getUser' }.returnType.name
    }

    void testDefaultConstructorBehaviourNotOverriden() {
        def entity = evaluate("""
          import grails.persistence.*
          @Entity
          class EntityTransformTest {

                boolean enabled
                int cash
                EntityTransformTest() {
                    enabled = true
                    cash = 30
                }
          }
          new EntityTransformTest()
        """)

        assert entity != null
        assert entity.enabled
        assert entity.cash == 30

    }
    void testAnnotatedEntity() {
        def entity = evaluate("""
          import grails.persistence.*
          @Entity
          class EntityTransformTest {

               static belongsTo = [one:EntityTransformTest]
               static hasMany = [many:EntityTransformTest]
          }
          new EntityTransformTest(id:1L, version:2L)
        """)


        assertEquals 1L, entity.id
        assertEquals 2L, entity.version

        entity.many = new HashSet()
        assertEquals 0, entity.many.size()

        entity.one = entity.class.newInstance()

        assertNotNull entity.one
    }

    void testToStringOverrideTests() {
        def entities = evaluate('''

        import grails.persistence.*
        @Entity
        class Personnel {
            String lastName
            String firstName
            String toString() {"${firstName}, ${lastName}"}
        }

        @Entity
        class Approver extends Personnel {

        }
        [new Approver(firstName:"joe", lastName:"bloggs"), new Personnel(firstName:"jack", lastName:"dee") ]
        ''')


        assertEquals "joe, bloggs", entities[0].toString()
        assertEquals "jack, dee", entities[1].toString()
    }
}
