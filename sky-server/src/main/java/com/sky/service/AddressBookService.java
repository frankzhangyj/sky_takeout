package com.sky.service;

import com.sky.entity.AddressBook;

import java.util.List;

/**
 * @Description
 * @Author frank
 * @Date 2024/10/30
 */
public interface AddressBookService {

    /**
     * 得到用户设置的所有地址
     * @return
     */
    List<AddressBook> getAddresses();

    /**
     * 得到用户默认地址
     * @return
     */
    List<AddressBook> getDefaultAddresses();

    /**
     * 保存地址信息
     * @param addressBook
     */
    void saveAddress(AddressBook addressBook);

    /**
     * 通过地址id得到地址信息
     * @param id
     * @return
     */
    AddressBook getById(Long id);

    /**
     * 更新地址信息
     * @param addressBook
     */
    void updateAddress(AddressBook addressBook);

    /**
     * 设置默认地址
     * @param addressBook
     */
    void setDefault(AddressBook addressBook);

    /**
     * 删除地址
     * @param id
     */
    void removeById(Long id);
}
