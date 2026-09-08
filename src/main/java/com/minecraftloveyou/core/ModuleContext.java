package com.minecraftloveyou.core;

/** 模块初始化上下文（后续放公共资源，如语言、子类共享服务）。 */
public record ModuleContext(boolean client) {
}