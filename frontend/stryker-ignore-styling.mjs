// Stryker ignorer: skips mutants that only change Tailwind classes or badge colours. Tests check what the page
// says and does, not its look, so such mutants would always survive without telling us anything.
import { declareValuePlugin, PluginKind } from "@stryker-mutator/api/plugin";

/** Constants that hold only class names or badge tones. */
const STYLE_CONSTANTS = /^(TONES?|linkClass|smallButton)$/;

function isStyling(path) {
  const node = path.node;
  if (node.type === "JSXAttribute" && node.name.name === "className") {
    return true;
  }
  if (node.type === "ObjectProperty" && node.key.type === "Identifier" && node.key.name === "tone") {
    return true;
  }
  if (node.type === "VariableDeclarator" && node.id.type === "Identifier" && STYLE_CONSTANTS.test(node.id.name)) {
    return true;
  }
  // The default tone of <Badge tone = "neutral">.
  return node.type === "AssignmentPattern" && node.left.type === "Identifier" && node.left.name === "tone";
}

export const strykerPlugins = [
  declareValuePlugin(PluginKind.Ignore, "styling", {
    shouldIgnore(path) {
      return isStyling(path) ? "Only changes Tailwind classes or badge colours" : undefined;
    },
  }),
];
