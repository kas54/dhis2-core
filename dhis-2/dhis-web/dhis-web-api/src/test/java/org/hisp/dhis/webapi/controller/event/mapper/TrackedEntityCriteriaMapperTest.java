package org.hisp.dhis.webapi.controller.event.mapper;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;

import org.apache.commons.lang.time.DateUtils;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.mock.MockCurrentUserService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.controller.event.TrackedEntityInstanceCriteria;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Sets;

/**
 * @author Luciano Fiandesio
 */
@WebAppConfiguration
public class TrackedEntityCriteriaMapperTest
    extends
    DhisSpringTest
{
    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private TrackedEntityAttributeService attributeService;

    @Autowired
    private UserService userService;

    @Autowired
    private TrackedEntityTypeService trackedEntityTypeService;

    @Autowired
    private TrackedEntityCriteriaMapper trackedEntityCriteriaMapper;

    private Program programA;

    private OrganisationUnit organisationUnit;

    private TrackedEntityType trackedEntityTypeA = createTrackedEntityType( 'A' );

    private TrackedEntityAttribute attrD = createTrackedEntityAttribute( 'D' );

    private TrackedEntityAttribute attrE = createTrackedEntityAttribute( 'E' );

    private TrackedEntityAttribute filtF = createTrackedEntityAttribute( 'F' );

    private TrackedEntityAttribute filtG = createTrackedEntityAttribute( 'G' );

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Override
    public void setUpTest()
    {
        organisationUnit = createOrganisationUnit( 'A' );
        organisationUnitService.addOrganisationUnit( organisationUnit );

        OrganisationUnit organisationUnitB = createOrganisationUnit( 'B' );
        organisationUnitService.addOrganisationUnit( organisationUnitB );

        programA = createProgram( 'A', new HashSet<>(), organisationUnit );
        programService.addProgram( programA );

        trackedEntityTypeA.setPublicAccess( AccessStringHelper.FULL );
        trackedEntityTypeService.addTrackedEntityType( trackedEntityTypeA );

        attributeService.addTrackedEntityAttribute( attrD );
        attributeService.addTrackedEntityAttribute( attrE );
        attributeService.addTrackedEntityAttribute( filtF );
        attributeService.addTrackedEntityAttribute( filtG );

        // mock user
        super.userService = this.userService;
        User user = createUser( "testUser" );
        user.setTeiSearchOrganisationUnits( Sets.newHashSet( organisationUnit ) );
        CurrentUserService currentUserService = new MockCurrentUserService( user );
        ReflectionTestUtils.setField( trackedEntityCriteriaMapper, "currentUserService", currentUserService );
    }

    @Test
    public void verifyCriteriaMapping()
    {
        TrackedEntityInstanceCriteria criteria = new TrackedEntityInstanceCriteria();
        criteria.setQuery( "query-test" );
        criteria.setAttribute( newHashSet( attrD.getUid(), attrE.getUid() ) );
        criteria.setFilter( newHashSet( filtF.getUid(), filtG.getUid() ) );
        criteria.setOu( organisationUnit.getUid() );
        criteria.setOuMode( OrganisationUnitSelectionMode.DESCENDANTS );
        criteria.setProgram( programA.getUid() );
        criteria.setProgramStatus( ProgramStatus.ACTIVE );
        criteria.setFollowUp( true );
        criteria.setLastUpdatedStartDate( getDate( 2019, 1, 1 ) );
        criteria.setLastUpdatedEndDate( getDate( 2020, 1, 1 ) );
        criteria.setLastUpdatedDuration( "20" );
        criteria.setProgramEnrollmentStartDate( getDate( 2019, 8, 5 ) );
        criteria.setProgramEnrollmentEndDate( getDate( 2020, 8, 5 ) );
        criteria.setProgramIncidentStartDate( getDate( 2019, 5, 5 ) );
        criteria.setProgramIncidentEndDate( getDate( 2020, 5, 5 ) );
        criteria.setTrackedEntityType( trackedEntityTypeA.getUid() );
        criteria.setEventStatus( EventStatus.COMPLETED );
        criteria.setEventStartDate( getDate( 2019, 7, 7 ) );
        criteria.setEventEndDate( getDate( 2020, 7, 7 ) );
        criteria.setAssignedUserMode( AssignedUserSelectionMode.PROVIDED );
        criteria.setAssignedUser( "user-1;user-2" );
        criteria.setSkipMeta( true );
        criteria.setPage( 1 );
        criteria.setPageSize( 50 );
        criteria.setTotalPages( false );
        criteria.setSkipPaging( false );
        criteria.setIncludeDeleted( true );
        criteria.setIncludeAllAttributes( true );
        criteria.setOrder( "order-1" );

        final TrackedEntityInstanceQueryParams queryParams = trackedEntityCriteriaMapper.map( criteria );

        assertThat( queryParams.getQuery().getFilter(), is( "query-test" ) );
        assertThat( queryParams.getQuery().getOperator(), is( QueryOperator.EQ ) );

        assertThat( queryParams.getProgram(), is( programA ) );
        assertThat( queryParams.getTrackedEntityType(), is( trackedEntityTypeA ) );
        assertThat( queryParams.getOrganisationUnits(), hasSize( 1 ) );
        assertThat( queryParams.getOrganisationUnits().iterator().next(), is( organisationUnit ) );
        assertThat( queryParams.getAttributes(), hasSize( 2 ) );
        assertTrue(
            queryParams.getAttributes().stream().anyMatch( a -> a.getItem().getUid().equals( attrD.getUid() ) ) );
        assertTrue(
            queryParams.getAttributes().stream().anyMatch( a -> a.getItem().getUid().equals( attrE.getUid() ) ) );

        assertThat( queryParams.getFilters(), hasSize( 2 ) );
        assertTrue( queryParams.getFilters().stream().anyMatch( a -> a.getItem().getUid().equals( filtF.getUid() ) ) );
        assertTrue( queryParams.getFilters().stream().anyMatch( a -> a.getItem().getUid().equals( filtG.getUid() ) ) );

        assertThat( queryParams.getPageSizeWithDefault(), is( 50 ) );
        assertThat( queryParams.getPageSize(), is( 50 ) );
        assertThat( queryParams.getPage(), is( 1 ) );
        assertThat( queryParams.isTotalPages(), is( false ) );

        assertThat( queryParams.getProgramStatus(), is( ProgramStatus.ACTIVE ) );
        assertThat( queryParams.getFollowUp(), is( true ) );

        assertThat( queryParams.getLastUpdatedStartDate(), is( criteria.getLastUpdatedStartDate() ) );
        assertThat( queryParams.getLastUpdatedEndDate(), is( criteria.getLastUpdatedEndDate() ) );

        assertThat( queryParams.getProgramEnrollmentStartDate(), is( criteria.getProgramEnrollmentStartDate() ) );
        assertThat( queryParams.getProgramEnrollmentEndDate(),
            is( DateUtils.addDays( criteria.getProgramEnrollmentEndDate(), 1 ) ) );

        assertThat( queryParams.getProgramIncidentStartDate(), is( criteria.getProgramIncidentStartDate() ) );
        assertThat( queryParams.getProgramIncidentEndDate(),
            is( DateUtils.addDays( criteria.getProgramIncidentEndDate(), 1 ) ) );

        assertThat( queryParams.getEventStatus(), is( EventStatus.COMPLETED ) );
        assertThat( queryParams.getEventStartDate(), is( criteria.getEventStartDate() ) );
        assertThat( queryParams.getEventEndDate(), is( criteria.getEventEndDate() ) );
        assertThat( queryParams.getAssignedUserSelectionMode(), is( AssignedUserSelectionMode.PROVIDED ) );
        assertTrue( queryParams.getAssignedUsers().stream().anyMatch( u -> u.equals( "user-1" ) ) );
        assertTrue( queryParams.getAssignedUsers().stream().anyMatch( u -> u.equals( "user-2" ) ) );

        assertThat( queryParams.isIncludeDeleted(), is( true ) );
        assertThat( queryParams.isIncludeAllAttributes(), is( true ) );

        assertTrue( queryParams.getOrders().stream().anyMatch( o -> o.equals( "order-1" ) ) );
    }

    @Test
    public void verifyCriteriaMappingFailOnMissingAttribute()
    {
        exception.expect( IllegalQueryException.class );
        exception.expectMessage( "Attribute does not exist: missing" );

        TrackedEntityInstanceCriteria criteria = new TrackedEntityInstanceCriteria();
        criteria.setAttribute( newHashSet( attrD.getUid(), attrE.getUid(), "missing" ) );

        trackedEntityCriteriaMapper.map( criteria );
    }

    @Test
    public void verifyCriteriaMappingFailOnMissingFilter()
    {
        exception.expect( IllegalQueryException.class );
        exception.expectMessage( "Attribute does not exist: missing" );

        TrackedEntityInstanceCriteria criteria = new TrackedEntityInstanceCriteria();
        criteria.setFilter( newHashSet( filtF.getUid(), filtG.getUid(), "missing" ) );

        trackedEntityCriteriaMapper.map( criteria );
    }

    @Test
    public void verifyCriteriaMappingFailOnMissingProgram()
    {
        exception.expect( IllegalQueryException.class );
        exception.expectMessage( "Program does not exist: " + programA.getUid() + "A" );

        TrackedEntityInstanceCriteria criteria = new TrackedEntityInstanceCriteria();
        criteria.setProgram( programA.getUid() + 'A' );

        trackedEntityCriteriaMapper.map( criteria );
    }

    @Test
    public void verifyCriteriaMappingFailOnMissingTrackerEntityType()
    {
        exception.expect( IllegalQueryException.class );
        exception.expectMessage( "Tracked entity type does not exist: " + trackedEntityTypeA.getUid() + "A" );

        TrackedEntityInstanceCriteria criteria = new TrackedEntityInstanceCriteria();
        criteria.setTrackedEntityType( trackedEntityTypeA.getUid() + "A" );

        trackedEntityCriteriaMapper.map( criteria );
    }

    @Test
    public void verifyCriteriaMappingFailOnMissingOrgUnit()
    {
        exception.expect( IllegalQueryException.class );
        exception.expectMessage( "Organisation unit does not exist: " + organisationUnit.getUid() + "A" );

        TrackedEntityInstanceCriteria criteria = new TrackedEntityInstanceCriteria();
        criteria.setOu( organisationUnit.getUid() + "A" );

        trackedEntityCriteriaMapper.map( criteria );
    }

    @Test
    public void verifyCriteriaMappingFailOnUserNonInOuHierarchy()
    {
        exception.expect( IllegalQueryException.class );
        exception.expectMessage( "Organisation unit is not part of the search scope: " + organisationUnit.getUid() );

        // Force Current User Service to return a User without search org unit
        ReflectionTestUtils.setField( trackedEntityCriteriaMapper, "currentUserService",
            new MockCurrentUserService( createUser( "testUser2" ) ) );
        TrackedEntityInstanceCriteria criteria = new TrackedEntityInstanceCriteria();
        criteria.setOu( organisationUnit.getUid() );

        trackedEntityCriteriaMapper.map( criteria );
    }

    @Test
    public void testGetFromUrlFailOnNonProvidedAndAssignedUsers()
    {
        exception.expect( IllegalQueryException.class );
        exception.expectMessage( "Assigned User uid(s) cannot be specified if selectionMode is not PROVIDED" );

        TrackedEntityInstanceCriteria criteria = new TrackedEntityInstanceCriteria();
        criteria.setAssignedUser( "user-1;user-2" );
        criteria.setAssignedUserMode( AssignedUserSelectionMode.CURRENT );

        trackedEntityCriteriaMapper.map( criteria );
    }
}