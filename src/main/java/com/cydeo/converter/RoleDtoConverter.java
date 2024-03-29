package com.cydeo.converter;

import com.cydeo.dto.RoleDTO;
import com.cydeo.service.RoleService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class RoleDtoConverter implements Converter<String, RoleDTO> {

    RoleService roleService;
    public RoleDtoConverter(RoleService roleService) {
        this.roleService = roleService;
    }
    @Override
    public RoleDTO convert(String id) {

        if (id == null || id.equals("")) {
            return null;
        }
        return roleService.findById(Long.parseLong(id));
    }

}
