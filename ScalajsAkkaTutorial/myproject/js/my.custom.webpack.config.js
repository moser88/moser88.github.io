module.exports = {
  "entry": {
    "myproject-fastopt": [__dirname + "/../../myproject-fastopt.js"]
  },
  "output": {
    "path": __dirname,
    "filename": "[name]-bundle.js"
  },
  "devtool": "source-map",
  "module": {
    "rules": [{
      "test": new RegExp("\\.js$"),
      "enforce": "pre",
      "use": ["source-map-loader"]
    }]
  }
}