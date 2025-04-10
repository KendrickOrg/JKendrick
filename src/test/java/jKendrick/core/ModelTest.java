package jKendrick.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ModelTest {
    @Test
    void testSetAndGetParameter() {
        Model model = new Model();
        model.setParameter("beta", 0.3);
        assertEquals(0.3, model.getParameter("beta"));
    }

    @Test
    void testUpdateParameter() {
        Model model = new Model();
        model.setParameter("gamma", 0.1);
        model.setParameter("gamma", 0.2);
        assertEquals(0.2, model.getParameter("gamma"));
    }

    @Test
    void testGetUnknownParameterReturnsZero() {
        Model model = new Model();
        assertEquals(0.0, model.getParameter("blablabla"));
    }

}