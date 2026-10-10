module.exports = {
  presets: [  // 编译语法预设(顺序从后往前)
    '@vue/babel-preset-app'
  ],
  plugins: [  // 编译时引入的插件
    '@babel/plugin-proposal-optional-chaining'
  ],
  /*overrides: [  // 覆盖编译指定文件的规则
    {
      test: "**!/page_react/components/!*.jsx",
      presets: [
        "@babel/preset-react",
      ],
      plugins: [
        "@babel/plugin-transform-react-jsx"
      ]
    },
  ]*/
}
