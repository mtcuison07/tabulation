/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package org.guanzon.tabulation;

/**
 *
 * @author micha
 */
public interface Callback {
    void MasterRetreive(int fnIndex, Object foValue);
    void DetailRetreive(int fnRow, int fnIndex, Object foValue);
}
