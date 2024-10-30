package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.sky.context.BaseContext;
import com.sky.entity.AddressBook;
import com.sky.mapper.AddressBookMapper;
import com.sky.service.AddressBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Description
 * @Author frank
 * @Date 2024/10/30
 */
@Service
public class AddressBookServiceImpl implements AddressBookService {
    @Autowired
    private AddressBookMapper addressBookMapper;

    @Override
    public List<AddressBook> getAddresses() {
        LambdaQueryWrapper<AddressBook> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AddressBook::getUserId, BaseContext.getCurrentId());
        List<AddressBook> addressBooks = addressBookMapper.selectList(queryWrapper);

        return addressBooks;
    }

    @Override
    public List<AddressBook> getDefaultAddresses() {
        LambdaQueryWrapper<AddressBook> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AddressBook::getUserId, BaseContext.getCurrentId())
                .eq(AddressBook::getDetail, 1);
        List<AddressBook> addressBooks = addressBookMapper.selectList(queryWrapper);

        return addressBooks;
    }

    @Override
    public void saveAddress(AddressBook addressBook) {
        addressBook.setIsDefault(0);
        addressBook.setUserId(BaseContext.getCurrentId());
        addressBookMapper.insert(addressBook);
    }

    @Override
    public AddressBook getById(Long id) {
        AddressBook addressBook = addressBookMapper.selectById(id);
        return addressBook;
    }

    @Override
    public void updateAddress(AddressBook addressBook) {
        addressBookMapper.updateById(addressBook);
    }

    @Override
    public void setDefault(AddressBook addressBook) {

        LambdaUpdateWrapper<AddressBook> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(AddressBook::getUserId, BaseContext.getCurrentId())
                        .set(AddressBook::getIsDefault, 0);
        addressBookMapper.update(addressBook, updateWrapper);

        addressBook.setIsDefault(1);
        addressBookMapper.updateById(addressBook);
    }

    @Override
    public void removeById(Long id) {
        addressBookMapper.deleteById(id);
    }
}
