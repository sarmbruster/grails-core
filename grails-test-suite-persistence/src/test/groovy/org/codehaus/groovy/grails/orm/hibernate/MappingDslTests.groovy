package org.codehaus.groovy.grails.orm.hibernate

import javax.sql.DataSource
import org.hibernate.type.TextType

 /**
 * @author Graeme Rocher
 * @since 1.0
 */
class MappingDslTests extends AbstractGrailsHibernateTests {

    void testTableMapping() {
        DataSource ds = applicationContext.dataSource

        def con
        try {
            con = ds.getConnection()
            con.prepareStatement("select * from people").execute()
        }
        finally {
            con.close()
        }
    }

    void testColumnNameMappings() {
        def p = ga.getDomainClass("PersonDSL").newInstance()
        p.firstName = "Wilma"
        p.save()
        session.flush()

        DataSource ds = applicationContext.dataSource

        def con
        try {
            con = ds.getConnection()
            def statement = con.prepareStatement("select First_Name from people")
            def result = statement.executeQuery()
            assertTrue result.next()
            def name = result.getString('First_Name')
            assertEquals "Wilma", name
        }
        finally {
            con.close()
        }
    }

    void testDisabledVersion() {
        def p = ga.getDomainClass("PersonDSL").newInstance()
        p.firstName = "Wilma"
        p.save()
        session.flush()

        assertNull p.version
    }

    void testEnabledVersion() {
        def p = ga.getDomainClass("PersonDSL2").newInstance()
        p.firstName = "Wilma"
        p.save()
        session.flush()

        assertEquals 0, p.version

        p.firstName = "Bob"
        p.save()
        session.flush()

        assertEquals 1, p.version
    }

    void testCustomHiLoIdGenerator() {
        def p = ga.getDomainClass("PersonDSL").newInstance()
        p.firstName = "Wilma"
        p.save()
        session.flush()

        assertNotNull p.id
        DataSource ds = applicationContext.dataSource

        def con
        try {
            con = ds.getConnection()
            def statement = con.prepareStatement("select * from hi_value")
            def result = statement.executeQuery()
            assertTrue result.next()
            def value = result.getLong('next_value')
            assertEquals 1, value
        }
        finally {
            con.close()
        }
    }

    void testLazyinessControl() {
        def personClass = ga.getDomainClass("PersonDSL")
        def p = personClass.newInstance()
        p.firstName = "Wilma"

        p.addToChildren(firstName:"Dino", lastName:'Dinosaur')
        p.addToCousins(firstName:"Bob", lastName:'The Builder')
        p.save(flush:true)
        session.clear()

        personClass.clazz.withNewSession {
            p = personClass.clazz.get(1)

            assertTrue p.children.wasInitialized()
            assertFalse p.cousins.wasInitialized()
        }
    }

    void testUserTypes() {
        DataSource ds = applicationContext.dataSource
        def relativeClass = ga.getDomainClass("Relative")
        def r = relativeClass.newInstance()
        r.firstName = "Wilma"
        r.lastName = 'Flintstone'
        r.save()
        session.flush()

        final cmd = session.getSessionFactory().getClassMetadata(relativeClass.clazz)

        final type = cmd.getPropertyType("firstName")

        assert type instanceof TextType
    }

    void testCompositeIdMapping() {
        def compositePersonClass = ga.getDomainClass("CompositePerson")
        def cp = compositePersonClass.newInstance()

        cp.firstName = "Fred"
        cp.lastName = "Flintstone"
        cp.save()
        session.flush()
        session.clear()

        cp = compositePersonClass.newInstance()
        cp.firstName = "Fred"
        cp.lastName = "Flintstone"

        def cp1 = compositePersonClass.clazz.get(cp)
        assertNotNull cp1
        assertEquals "Fred", cp1.firstName
        assertEquals "Flintstone", cp1.lastName
    }

    void testTablePerSubclassInheritance() {
        DataSource ds = applicationContext.dataSource

        def con
        try {
            con = ds.getConnection()
            con.prepareStatement("select * from payment").execute()
            con.prepareStatement("select * from credit_card_payment").execute()
        }
        finally {
            con.close()
        }

        def p = ga.getDomainClass("Payment").newInstance()
        p.amount = 10
        p.save()
        session.flush()

        def ccp = ga.getDomainClass("CreditCardPayment").newInstance()
        ccp.amount = 20
        ccp.cardNumber = "43438094834380"
        ccp.save()
        session.flush()
        session.clear()

        ccp = ga.getDomainClass("CreditCardPayment").clazz.findByAmount(20)
        assertNotNull ccp
        assertEquals 20, ccp.amount
        assertEquals  "43438094834380", ccp.cardNumber
    }

    void testOneToOneForeignKeyMapping() {
        def personClass = ga.getDomainClass("MappedPerson").clazz
        def addressClass = ga.getDomainClass("MappedAddress").clazz

        def p = personClass.newInstance(name:"John")
        p.address = addressClass.newInstance()

        assertNotNull p.save()
        session.flush()

        DataSource ds = applicationContext.dataSource
        def con
        try {
            con = ds.getConnection()
            def statement = con.prepareStatement("select PERSON_ADDRESS_COLUMN from mapped_person")
            def resultSet = statement.executeQuery()
            assert resultSet.next()
        }
        finally {
            con.close()
        }
    }

    void testManyToOneForeignKeyMapping() {
        def personClass = ga.getDomainClass("MappedPerson").clazz
        def groupClass = ga.getDomainClass("MappedGroup").clazz

        def g = groupClass.newInstance()
        g.addToPeople name:"John"
        assertNotNull g.save()

        session.flush()
        session.clear()

        g = groupClass.get(1)
        assertNotNull g
        assertEquals 1, g.people.size()

        DataSource ds = applicationContext.dataSource
        def con
        try {
            con = ds.getConnection()
            def statement = con.prepareStatement("select PERSON_GROUP_COLUMN from mapped_person")
            def resultSet = statement.executeQuery()
            assertTrue resultSet.next()
        }
        finally {
            con.close()
        }
    }

    void testManyToManyForeignKeyMapping() {
        def partnerClass = ga.getDomainClass("MappedPartner").clazz
        def groupClass = ga.getDomainClass("MappedGroup").clazz

        def g = groupClass.newInstance()
        g.addToPartners(partnerClass.newInstance())

        assertNotNull g.save()
        session.flush()
        session.clear()

        g = groupClass.get(1)
        assertNotNull g
        assertEquals 1, g.partners.size()

        DataSource ds = applicationContext.dataSource
        def con
        try {
            con = ds.getConnection()
            def statement = con.prepareStatement("select PARTNER_JOIN_COLUMN,GROUP_JOIN_COLUMN from PARTNER_GROUP_ASSOCIATIONS")
            def resultSet = statement.executeQuery()
            assertTrue resultSet.next()
        }
        finally {
            con?.close()
        }
    }

    void testUnidirectionalOneToManyForeignKeyMapping() {
        def personClass = ga.getDomainClass("MappedPerson").clazz
        def childClass = ga.getDomainClass("MappedChild").clazz

        def p = personClass.newInstance(name:"John")
        p.addToChildren(childClass.newInstance())
        p.addToCousins(childClass.newInstance())
        p.save()

        assertNotNull p.save()
        session.flush()

        DataSource ds = applicationContext.dataSource
        def con
        try {
            con = ds.getConnection()
            def statement = con.prepareStatement("select PERSON_ID,COUSIN_ID from COUSINS_TABLE")
            def resultSet = statement.executeQuery()
            assertTrue resultSet.next()
        }
        finally {
            con.close()
        }
    }

    void testCompositeIdAssignedGenerator_GRAILS_6289() {
        def ds = applicationContext.dataSource
        def con = ds.connection
        def stmt = con.createStatement()
        stmt.executeUpdate "INSERT INTO Composite_Id_Assigned_AUTHOR VALUES('Frank Herbert',0)"
        stmt.executeUpdate "INSERT INTO Composite_Id_Assigned_BOOK VALUES('first','Frank Herbert',0)"
        stmt.executeUpdate "INSERT INTO Composite_Id_Assigned_BOOK VALUES('second','Frank Herbert',0)"
        con.close()

        def authorClass = ga.getDomainClass('CompositeIdAssignedAuthor').clazz

        // per GRAILS-6289, this will throw an exception because the afterLoad property cannot be found...
        authorClass.executeQuery 'select distinct a from CompositeIdAssignedAuthor as a inner join fetch a.books'
    }

    void testEnumIndex() {
        DataSource ds = applicationContext.dataSource
        List<String> indexNames = []
        def connection
        try {
            connection = ds.getConnection()
            def rs = connection.metaData.getIndexInfo(null, null, 'ENUM_INDEXED', false, false)
            while (rs.next()) { indexNames << rs.getString('INDEX_NAME') }
            rs.close()
        }
        finally {
            connection.close()
        }

        assertTrue indexNames.contains('NAME_INDEX')
        assertTrue indexNames.contains('TRUTH_INDEX')
    }

    protected void onSetUp() {
        gcl.parseClass '''
import grails.persistence.*

@Entity
class MappedPerson {
    String name
    MappedAddress address
    MappedGroup group
    Set children
    Set cousins

    static hasMany = [children:MappedChild, cousins:MappedChild]
    static belongsTo = MappedGroup
    static mapping = {
        columns {
            address column:'PERSON_ADDRESS_COLUMN'
            group column:'PERSON_GROUP_COLUMN'
            children column:'PERSON_CHILD_ID'
            cousins joinTable:[name:'COUSINS_TABLE', key:'PERSON_ID', column:'COUSIN_ID']
        }
    }
    static constraints = {
        group(nullable:true)
        address(nullable:true)
    }
}


@Entity
class MappedChild {
}

@Entity
class MappedAddress {

    static belongsTo = MappedPerson
}

@Entity
class MappedGroup {

    static hasMany = [people:MappedPerson, partners:MappedPartner]
    static mapping = {
        columns {
            partners column:'PARTNER_JOIN_COLUMN', joinTable:'PARTNER_GROUP_ASSOCIATIONS'
        }
    }
}

@Entity
class MappedPartner {
    static belongsTo = MappedGroup
    static hasMany = [groups:MappedGroup]
    static mapping = {
        columns {
            groups column:'GROUP_JOIN_COLUMN', joinTable:'PARTNER_GROUP_ASSOCIATIONS'
        }
    }
}

@Entity
class Payment {
    Integer amount

    static mapping = {
        tablePerHierarchy false
    }
}

@Entity
class CreditCardPayment extends Payment {
    String cardNumber
}

@Entity
class CompositePerson implements Serializable {
    String firstName
    String lastName

    static mapping = {
        id composite:['firstName', 'lastName']
    }
}

@Entity
class PersonDSL {
    String firstName

    static hasMany = [children:Relative, cousins:Relative]
    static mapping = {
        table 'people'
        version false
        cache usage:'read-only', include:'non-lazy'
        id generator:'hilo', params:[table:'hi_value',column:'next_value',max_lo:100]

        columns {
            firstName name:'First_Name'
            children lazy:false, cache:'read-write\'
        }
    }
}

@Entity
class Relative {

    String firstName
    String lastName

    static mapping = {
        columns {
            firstName type:'text', index:'name_index'
            lastName index:'name_index,other_index'
        }
    }
}

enum Truth {
    TRUE,
    FALSE
}

@Entity
class EnumIndexed {

    String name
    Truth truth

    static mapping = {
        name index: 'name_index'
        truth index: 'truth_index'
    }
}

@Entity
class PersonDSL2 {
    String firstName

    static mapping = {
        table 'people2'
        version true
        cache usage:'read-write', include:'non-lazy'
        id column:'person_id'

        columns {
            firstName name:'First_Name'
        }
    }
}
@grails.persistence.Entity
class CompositeIdAssignedAuthor {
    String id
    static hasMany = [books:CompositeIdAssignedBook]

    static mapping = {
       id column: 'name', generator:'assigned'
    }
}

@grails.persistence.Entity
class CompositeIdAssignedBook implements Serializable {
   String edition
   CompositeIdAssignedAuthor author

   static mapping = {
      id composite:['edition','author']
   }
}
'''
    }
}
