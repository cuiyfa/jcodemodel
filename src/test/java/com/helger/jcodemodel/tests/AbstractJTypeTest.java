/*
 * Copyright 2014 Philip Helger.
 */
package com.helger.jcodemodel.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.junit.After;
import org.junit.Test;

import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.JCodeModel;

/**
 * @author Victor Nazarov <asviraspossible@gmail.com>
 */
public class AbstractJTypeTest
{
  private static class AssignmentTypes
  {
    private final AbstractJClass _variable;
    private final AbstractJClass _value;

    public AssignmentTypes (final AbstractJClass aVariable, final AbstractJClass aValue)
    {
      _variable = aVariable;
      _value = aValue;
    }
  }

  private List <AbstractJClass> freshTypes = new ArrayList <AbstractJClass> ();
  private List <AssignmentTypes> freshAssignableTypes = new ArrayList <AssignmentTypes> ();

  @Nonnull
  private void _registerType (final AbstractJClass type)
  {
    freshTypes.add (type);
  }

  @Nonnull
  private List <AbstractJClass> _refreshTypes ()
  {
    final List <AbstractJClass> result = freshTypes;
    freshTypes = new ArrayList <AbstractJClass> ();
    return result;
  }

  @Nonnull
  private List <AssignmentTypes> _refreshAssignableTypes ()
  {
    final List <AssignmentTypes> result = freshAssignableTypes;
    freshAssignableTypes = new ArrayList <AssignmentTypes> ();
    return result;
  }

  private void _assertIsAssignableInTopLevelPositionOnly (final AbstractJClass variable, final AbstractJClass value)
  {
    final boolean result = variable.isAssignableFrom (value);
    // System.out.println(variable + ".isAssignableFrom(" + value + ") == " +
    // result);
    assertTrue ("Expecting " + variable + " to be assignable from " + value, result);
  }

  private void _assertIsAssignable (final AbstractJClass variable, final AbstractJClass value)
  {
    freshAssignableTypes.add (new AssignmentTypes (variable, value));
    _assertIsAssignableInTopLevelPositionOnly (variable, value);
  }

  private void _assertIsNotAssignable (final AbstractJClass variable, final AbstractJClass value)
  {
    final boolean result = variable.isAssignableFrom (value);
    // System.out.println(variable + ".isAssignableFrom (" + value + ") == " +
    // result);
    assertFalse ("Expecting " + variable + " not to be assignable from " + value, result);
  }

  @After
  public void cleanup ()
  {
    _refreshTypes ();
    _refreshAssignableTypes ();
  }

  @Test
  public void testIsAssignableFromSmoke ()
  {
    final JCodeModel codeModel = new JCodeModel ();
    final AbstractJClass _Object = codeModel.ref (Object.class);
    final AbstractJClass _Integer = codeModel.ref (Integer.class);
    final AbstractJClass _List = codeModel.ref (List.class);

    _assertIsAssignable (_Object, _Integer);
    _assertIsNotAssignable (_Integer, _Object);
    _assertIsNotAssignable (_Integer, _List);
    _assertIsNotAssignable (_List, _Integer);
    _assertIsAssignable (_Object, _List);
    _assertIsNotAssignable (_List, _Object);

    _assertIsAssignable (_List.narrow (_Integer), _List.narrow (_Integer));
    _assertIsNotAssignable (_List.narrow (_Object), _List.narrow (_Integer));
    _assertIsNotAssignable (_List.narrow (_Integer), _List.narrow (_Object));
    _assertIsAssignable (_List.narrow (_Object.wildcard ()), _List.narrow (_Integer));
    _assertIsAssignable (_List.narrow (_Object.wildcard ()), _List.narrow (_Integer.wildcard ()));
    _assertIsAssignable (_List.narrow (_Integer.wildcardSuper ()), _List.narrow (_Object));
    _assertIsAssignable (_List.narrow (_Integer.wildcardSuper ()), _List.narrow (_Object.wildcardSuper ()));
    _assertIsNotAssignable (_List.narrow (_Integer.wildcardSuper ()), _List.narrow (_Integer.wildcard ()));
    _assertIsNotAssignable (_List.narrow (_Integer.wildcard ()), _List.narrow (_Integer.wildcardSuper ()));

    _assertIsNotAssignable (_List.narrow (_List), _List.narrow (_List.narrow (_Integer)));
    _assertIsAssignable (_List.narrow (_List.wildcard ()), _List.narrow (_List.narrow (_Integer)));

    // List<? extends List<Object>> list1 = (List<List>)list2
    _assertIsNotAssignable (_List.narrow (_List.narrow (_Object).wildcard ()), _List.narrow (_List));

    // List<? super List<List<List>>> list1 = (List<List<? super List>>)list2
    _assertIsNotAssignable (_List.narrow (_List.narrow (_List.narrow (_List)).wildcardSuper ()),
                            _List.narrow (_List.narrow (_List.wildcardSuper ())));
  }

  @Test
  public void testIsAssignableFromRandomized ()
  {
    final JCodeModel codeModel = new JCodeModel ();
    final AbstractJClass _Object = codeModel.ref (Object.class);
    final AbstractJClass _Integer = codeModel.ref (Integer.class);
    final AbstractJClass _List = codeModel.ref (List.class);

    _registerType (_Object);
    _registerType (_Integer);
    _registerType (_List);

    for (int i = 0; i < 2; i++)
    {
      for (final AbstractJClass type : _refreshTypes ())
      {
        _assertIsAssignable (_Object, type);
        _assertIsAssignable (type, type);

        _registerType (_List.narrow (type));
        _registerType (_List.narrow (type.wildcard ()));
        _registerType (_List.narrow (type.wildcardSuper ()));

        _assertIsAssignableInTopLevelPositionOnly (_List.narrow (type), _List);
        _assertIsAssignable (_List, _List.narrow (type));

        _assertIsAssignable (_List.narrow (type), _List.narrow (type));
        _assertIsAssignable (_List.narrow (type.wildcard ()), _List.narrow (type));
        _assertIsNotAssignable (_List.narrow (type.wildcard ()), _List.narrow (type.wildcardSuper ()));
        _assertIsNotAssignable (_List.narrow (type.wildcardSuper ()), _List.narrow (type.wildcard ()));
        _assertIsAssignable (_List.narrow (type.wildcardSuper ()), _List.narrow (type));
      }
      for (final AssignmentTypes assignment : _refreshAssignableTypes ())
      {
        if (!assignment._value.equals (assignment._variable))
        {
          _assertIsNotAssignable (_List.narrow (assignment._variable), _List.narrow (assignment._value));
        }
        _assertIsAssignable (_List.narrow (assignment._variable.wildcard ()), _List.narrow (assignment._value));
        _assertIsAssignable (_List.narrow (assignment._variable.wildcard ()),
                             _List.narrow (assignment._value.wildcard ()));
        _assertIsAssignable (_List.narrow (assignment._value.wildcardSuper ()), _List.narrow (assignment._variable));
        _assertIsAssignable (_List.narrow (assignment._value.wildcardSuper ()),
                             _List.narrow (assignment._variable.wildcardSuper ()));
      }
    }
  }
}